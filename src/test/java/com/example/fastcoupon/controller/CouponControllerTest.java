package com.example.fastcoupon.controller;

import com.example.fastcoupon.entity.CouponIssue;
import com.example.fastcoupon.entity.User;
import com.example.fastcoupon.enums.UserRoleEnum;
import com.example.fastcoupon.repository.CouponIssueRepository;
import com.example.fastcoupon.repository.CouponRepository;
import com.example.fastcoupon.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private CouponRepository couponRepository;
    @Autowired private CouponIssueRepository couponIssueRepository;

    private Long couponId;

    @BeforeEach
    void setup() {
        // ID를 하드코딩하지 않는다. ddl-auto=create 이후 AUTO_INCREMENT 상태에 따라 실제 ID가 달라진다.
        couponId = couponRepository.save(
                com.example.fastcoupon.entity.Coupon.createCoupon("테스트 쿠폰",
                        com.example.fastcoupon.enums.CouponTypeEnum.CHICKEN,
                        100,
                        LocalDateTime.now().plusDays(3)
                )
        ).getId();
        deleteCouponKeys(); // 다른 테스트가 같은 ID로 남긴 done/running 키가 워커를 막지 않도록

        redisTemplate.opsForSet().add("coupon:active:ids", String.valueOf(couponId));

        // RedisCouponService.getTotalCount()는 DB 폴백 없이 Redis 값만 보기 때문에,
        // AdminCouponService.createCoupon()이 하는 것과 동일하게 total/expire를 직접
        // 세팅해야 한다. total이 없으면 pushQueue()가 "재고 소진"으로 즉시 거부하고,
        // expire가 없으면 tryIssueCoupon()의 Lua 스크립트가 TTL 0으로 SET을 시도해 실패한다.
        redisTemplate.opsForValue().set("coupon:" + couponId + ":total", "100");
        redisTemplate.opsForValue().set("coupon:" + couponId + ":expire", "", Duration.ofMinutes(10));
    }

    @AfterEach
    void cleanup() {
        deleteCouponKeys(); // done/running까지 지운다. 남기면 같은 ID를 받는 다음 테스트의 워커가 건너뛴다.
        redisTemplate.opsForSet().remove("coupon:active:ids", String.valueOf(couponId));

        couponIssueRepository.deleteAllInBatch();
        couponRepository.deleteAllInBatch();
    }

    private void deleteCouponKeys() {
        Set<String> keys = redisTemplate.keys("coupon:" + couponId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @DisplayName("250명의 유저가 동시에 발급 요청 시 정확히 100명만 발급된다")
    @Test
    void 인증된_250명의_유저가_동시에_쿠폰발급_요청하면_100명만_발급된다() throws Exception {
        // given
        int totalUsers = 500;
        ExecutorService service = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(totalUsers);

        // when
        for (long userId = 0; userId < totalUsers; userId++) {
            final long uid = userId;
            service.execute(() -> {
                try {
                    User user = User.builder().id(uid).email(uid + "@test.com").role(UserRoleEnum.USER).build();
                    UserDetailsImpl userDetails = new UserDetailsImpl(user, user.getEmail());

                    mockMvc.perform(post("/api/coupons/{couponId}/issue", couponId)
                                    .with(user(userDetails)))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    System.out.println("❌ 실패: userId=" + uid + " → " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        service.shutdown();
        System.out.println("✅ 모든 요청 완료");

        // then
        Awaitility.await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            int issuedCount = couponIssueRepository.findAll().size();
            System.out.println("✅ 발급된 쿠폰 수: " + issuedCount);
            assertEquals(100, issuedCount);
        });

        String countKey = "coupon:" + couponId + ":count";
        String countValue = redisTemplate.opsForValue().get(countKey);
        System.out.println("✅ Redis 쿠폰 발급 수량(countKey): " + countValue);

        Set<String> redisKeys = redisTemplate.keys("coupon:" + couponId + ":user:*");
        System.out.println("✅ Redis 발급된 유저 수: " + redisKeys.size());
        assertEquals(100, redisKeys.size());

        couponIssueRepository.findAll()
                .stream()
                .map(CouponIssue::getUserId)
                .sorted()
                .forEach(id -> System.out.println("✅ 발급된 userId: " + id));
    }
}