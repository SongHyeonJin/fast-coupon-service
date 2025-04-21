package com.example.fastcoupon.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID = "traceId";

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {

        String traceId = MDC.get(TRACE_ID);
        String method = request.getMethod();
        String uri = request.getRequestURI();

        if (response instanceof ContentCachingResponseWrapper wrapper) {
            String responseBody = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            log.info("✅ ResponseBody: [{}] [{} {}] {} {}\nBody: {}", traceId, method, uri, response.getStatus(), response.getContentType(), responseBody);
        } else {
            log.warn("Response body unavailable — wrapper missing");
        }

        MDC.clear();
    }
}
