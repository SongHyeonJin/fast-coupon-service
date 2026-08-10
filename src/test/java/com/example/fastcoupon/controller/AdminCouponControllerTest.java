package com.example.fastcoupon.controller;

import com.example.fastcoupon.entity.User;
import com.example.fastcoupon.enums.UserRoleEnum;
import com.example.fastcoupon.repository.CouponRepository;
import com.example.fastcoupon.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCouponControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CouponRepository couponRepository;

    private MockHttpSession adminSession;

    @BeforeEach
    void setup() {
        User admin = userRepository.save(
                User.builder()
                        .name("관리자")
                        .email("admin@test.com")
                        .password("encoded")
                        .tel("010-0000-0000")
                        .role(UserRoleEnum.ADMIN)
                        .build()
        );

        // SessionAuthenticationFilter가 SecurityContext가 아니라 HttpSession의
        // userId/email/role 속성을 직접 읽어 /api/admin/** 권한을 판별하므로,
        // 로그인 시 LoginSessionAspect가 세팅하는 것과 동일한 속성을 미리 넣어둔다.
        adminSession = new MockHttpSession();
        adminSession.setAttribute("userId", admin.getId());
        adminSession.setAttribute("email", admin.getEmail());
        adminSession.setAttribute("role", UserRoleEnum.ADMIN);
    }

    @AfterEach
    void tearDown() {
        couponRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("발급 수량이 0 이하면 400을 반환한다")
    @Test
    void createCoupon_수량이_0이하면_실패() throws Exception {
        String body = """
                {"name":"테스트 쿠폰","type":"CHICKEN","totalQuantity":0,"expiredAt":"%s"}
                """.formatted(LocalDateTime.now().plusDays(3));

        mockMvc.perform(post("/api/admin/coupons")
                        .session(adminSession)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("만료일시가 과거면 400을 반환한다")
    @Test
    void createCoupon_만료일시가_과거면_실패() throws Exception {
        String body = """
                {"name":"테스트 쿠폰","type":"CHICKEN","totalQuantity":10,"expiredAt":"2020-01-01T00:00:00"}
                """;
        mockMvc.perform(post("/api/admin/coupons")
                        .session(adminSession)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("유효한 요청은 정상적으로 쿠폰을 생성한다")
    @Test
    void createCoupon_유효한_요청은_성공() throws Exception {
        String body = """
                {"name":"테스트 쿠폰","type":"CHICKEN","totalQuantity":10,"expiredAt":"%s"}
                """.formatted(LocalDateTime.now().plusDays(3));

        mockMvc.perform(post("/api/admin/coupons")
                        .session(adminSession)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
    }
}
