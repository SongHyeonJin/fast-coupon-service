package com.example.fastcoupon.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ExceptionEnum {
    EMAIL_DUPLICATION(HttpStatus.BAD_REQUEST.value(),"USER", "중복된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "USER", "사용자를 찾을 수 없습니다."),
    WRONG_PASSWORD(HttpStatus.BAD_REQUEST.value(), "USER", "비밀번호가 일치하지 않습니다."),
    NOT_ALLOW(HttpStatus.FORBIDDEN.value(), "USER", "권한이 없습니다."),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "COUPON", "해당 쿠폰이 존재하지 않습니다."),
    COUPON_ALREADY_ISSUED(HttpStatus.BAD_REQUEST.value(), "COUPON", "이미 해당 쿠폰을 발급받았습니다."),
    COUPON_OUT_OF_STOCK(HttpStatus.BAD_REQUEST.value(), "COUPON", "남은 쿠폰 수량이 없습니다."),
    COUPON_ALREADY_USED(HttpStatus.BAD_REQUEST.value(), "COUPON", "이미 사용된 쿠폰입니다."),
    LOCK_ACQUISITION_FAILED(HttpStatus.TOO_MANY_REQUESTS.value(), "COUPON", "요청이 몰려 있어 잠시 후 다시 시도해주세요."),
    INTERRUPTED_DURING_LOCK(HttpStatus.INTERNAL_SERVER_ERROR.value(), "COUPON", "처리 중 인터럽트가 발생했습니다."),
    COUPON_LUA_UNEXPECTED_RESULT(HttpStatus.INTERNAL_SERVER_ERROR.value(), "COUPON", "쿠폰 발급 중 알 수 없는 오류가 발생했습니다."),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST.value(), "COUPON", "만료된 쿠폰은 사용할 수 없습니다.");

    private final int status;
    private final String type;
    private final String msg;

    ExceptionEnum(int status, String type, String msg) {
        this.status = status;
        this.type = type;
        this.msg = msg;
    }
}
