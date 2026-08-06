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

    @BeforeEach
    void setup() {
        // 쿠폰 등록 (ID = 1)
        if (!couponRepository.existsById(1L)) {
            couponRepository.save(
                    com.example.fastcoupon.entity.Coupon.createCoupon("테스트 쿠폰",
                            com.example.fastcoupon.enums.CouponTypeEnum.CHICKEN,
                            100,
                            LocalDateTime.now().plusDays(3)
                    )
            );
        }
        redisTemplate.opsForSet().add("coupon:active:ids", "1");
    }

    @AfterEach
    void cleanup() {
        redisTemplate.delete("coupon:1:count");
        redisTemplate.delete("coupon:1:queue");
        redisTemplate.delete("coupon:1:total");

        Set<String> keys = redisTemplate.keys("coupon:1:user:*");
        if (keys != null) {
            redisTemplate.delete(keys);
        }

        couponRepository.deleteAllInBatch();
        couponIssueRepository.deleteAllInBatch();
    }

    @DisplayName("250명의 유저가 동시에 발급 요청 시 정확히 100명만 발급된다")
    @Test
    void 인증된_250명의_유저가_동시에_쿠폰발급_요청하면_100명만_발급된다() throws Exception {
        // given
        int totalUsers = 500;
        Long couponId = 1L;
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