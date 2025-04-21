package com.example.fastcoupon.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ExceptionEnum {
    EMAIL_DUPLICATION(HttpStatus.BAD_REQUEST.value(),"USER", "중복된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST.value(), "USER", "사용자를 찾을 수 없습니다."),
    WRONG_PASSWORD(HttpStatus.BAD_REQUEST.value(), "USER", "비밀번호가 일치하지 않습니다."),
    NOT_ALLOW(HttpStatus.FORBIDDEN.value(), "권한이 없습니다.", "USER");

    private final int status;
    private final String type;
    private final String msg;

    ExceptionEnum(int status, String type, String msg) {
        this.status = status;
        this.type = type;
        this.msg = msg;
    }
}
