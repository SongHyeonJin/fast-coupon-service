package com.example.fastcoupon.redis;

import com.example.fastcoupon.entity.Coupon;
import com.example.fastcoupon.enums.CouponTypeEnum;
import com.example.fastcoupon.repository.CouponRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RedisCouponServiceTest {

    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private CouponRepository couponRepository;
    @Autowired private RedisCouponService redisCouponService;

    private static final String LUA_SCRIPT = """
          local userKey = KEYS[1]
          local countKey = KEYS[2]
          local total = tonumber(ARGV[1])
          
          -- 이미 발급받은 유저인지 확인
          if redis.call("EXISTS", userKey) == 1 then
              return 1 -- 중복 발급
          end
          
          -- 현재 발급된 수 확인 (get으로 count 조회)
          local current = tonumber(redis.call("GET", countKey) or "0")
          if current >= total then
              return 2 -- 재고 없음
          end
          
          -- 발급 처리
          redis.call("INCR", countKey)
          redis.call("SET", userKey, "true", "EX", 300)
          return 0 -- 발급 성공
    """;
    private final String userKey = "coupon:1:user:999";
    private final String countKey = "coupon:1:count";

    @BeforeEach
    void setup() {
        Coupon coupon = Coupon.createCoupon(
                "테스트 쿠폰",
                CouponTypeEnum.CHICKEN,
                100,
                LocalDateTime.now().plusDays(3)
        );
        couponRepository.save(coupon);
    }

    @AfterEach
    void cleanup() {
        redisTemplate.delete(Arrays.asList(
                "coupon:1:queue", "coupon:1:total", "coupon:1:count"
        ));
        Set<String> userKeys = redisTemplate.keys("coupon:1:user:*");
        if (userKeys != null) redisTemplate.delete(userKeys);

        couponRepository.deleteAllInBatch();
    }

    @DisplayName("Redis Lua 스크립트가 중복/재고를 정확히 검증한다")
    @Test
    void redis_Lua_스크립트_정상_동작_테스트() {
        // given
        int total = 100;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);

        // when
        Long result = redisTemplate.execute(
                script,
                Arrays.asList(userKey, countKey),
                String.valueOf(total)
        );

        // then
        System.out.println("✅ Lua 실행 결과: " + result);
        assertThat(result).isEqualTo(0);
        assertThat(redisTemplate.hasKey(userKey)).isTrue();
        assertThat(redisTemplate.opsForValue().get(countKey)).isEqualTo("1");
    }

    @DisplayName("이미 발급된 유저는 Redis 큐에 들어가지 않는다")
    @Test
    void 중복_유저_큐_삽입_방지_테스트() {
        // given
        Long couponId = 1L, userId = 999L;
        redisTemplate.opsForValue().set(userKey, "true");

        // when
        redisCouponService.pushQueue(couponId, userId);

        // then
        String queueKey = "coupon:" + couponId + ":queue";
        Long queueSize = redisTemplate.opsForList().size(queueKey);
        assertThat(queueSize).isEqualTo(0);
    }

    @DisplayName("Redis 큐에서 발급 요청을 순서대로 꺼낸다 (blockingPopQueue)")
    @Test
    void blockingPopQueue_정상동작_테스트() {
        // given
        Long couponId = 1L;
        String queueKey = "coupon:" + couponId + ":queue";
        redisTemplate.opsForList().rightPush(queueKey, "1:100");
        redisTemplate.opsForList().rightPush(queueKey, "1:101");

        // when
        String first = redisCouponService.blockingPopQueue(couponId);
        String second = redisCouponService.blockingPopQueue(couponId);
        String third = redisCouponService.blockingPopQueue(couponId);

        // then
        assertThat(first).isEqualTo("1:100");
        assertThat(second).isEqualTo("1:101");
        assertThat(third).isNull();
    }

    @DisplayName("Redis에 쿠폰 수량 정보가 없으면 DB에서 조회해 저장한다")
    @Test
    void getTotalCount_레디스에없을때_DB에서가져와캐싱_테스트() {
        // given
        Coupon coupon = Coupon.createCoupon(
                "7000원 할인 쿠폰",
                CouponTypeEnum.PIZZA,
                50,
                LocalDateTime.now().plusDays(3));
        coupon = couponRepository.save(coupon);
        Long couponId = coupon.getId();

        redisTemplate.delete("coupon:" + couponId + ":total");

        // when
        int totalCount = redisCouponService.getTotalCount(couponId);

        // then
        assertThat(totalCount).isEqualTo(50);
        String redisCached = redisTemplate.opsForValue().get("coupon:" + couponId + ":total");
        assertThat(redisCached).isEqualTo("50");
    }

}