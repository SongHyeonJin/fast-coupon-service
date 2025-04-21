package com.example.fastcoupon.aop;

import com.example.fastcoupon.dto.user.UserResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoginSessionAspect {

    @Around("@annotation(com.example.fastcoupon.aop.LoginSessionInject)")
    public Object injectSession(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        if (result instanceof ResponseEntity<?> responseEntity) {
            Object body = responseEntity.getBody();

            if (body instanceof UserResponseDto dto) {
                request.getSession().setAttribute("userId", dto.getUserId());
                log.info("✅ 세션 저장 userId = {}", dto.getUserId());
            }
        }

        return result;
    }

}
