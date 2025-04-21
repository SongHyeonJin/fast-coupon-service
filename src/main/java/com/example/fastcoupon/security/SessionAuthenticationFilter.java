package com.example.fastcoupon.security;

import com.example.fastcoupon.entity.User;
import com.example.fastcoupon.enums.ExceptionEnum;
import com.example.fastcoupon.enums.UserRoleEnum;
import com.example.fastcoupon.exception.ErrorException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        Object userIdObj = request.getSession().getAttribute("userId");
        Object emailObj = request.getSession().getAttribute("email");
        Object roleObj = request.getSession().getAttribute("role");

        if (userIdObj != null && emailObj != null && roleObj instanceof UserRoleEnum role) {
            User dummyUser = User.builder()
                    .id((Long) userIdObj)
                    .email((String) emailObj)
                    .role(role)
                    .build();

            UserDetailsImpl userDetails = new UserDetailsImpl(dummyUser, dummyUser.getEmail());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        if (uri.startsWith("/api/admin")) {
            if (!(roleObj instanceof UserRoleEnum role) || !UserRoleEnum.ADMIN.equals(role)) {
                log.warn("❌ 권한 없음! roleObj = {}", roleObj);
                throw new ErrorException(ExceptionEnum.NOT_ALLOW);
            }
        }
        filterChain.doFilter(request, response);
    }
}
