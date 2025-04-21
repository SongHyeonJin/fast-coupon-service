package com.example.fastcoupon.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ExceptionEnum {
    EMAIL_DUPLICATION(HttpStatus.BAD_REQUEST.value(),"USER", "중복된 이메일입니다.");

    private final int status;
    private final String type;
    private final String msg;

    ExceptionEnum(int status, String type, String msg) {
        this.status = status;
        this.type = type;
        this.msg = msg;
    }
}
