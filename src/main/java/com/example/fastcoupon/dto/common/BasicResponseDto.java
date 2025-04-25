package com.example.fastcoupon.dto.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@NoArgsConstructor
public class BasicResponseDto {

    private int statusCode;
    private String msg;
    private List<String> errorMessages;

    public BasicResponseDto(int statusCode, String msg) {
        this.statusCode = statusCode;
        this.msg = msg;
    }

    public BasicResponseDto(int statusCode, String msg, List<String> errorMessages) {
        this.statusCode = statusCode;
        this.msg = msg;
        this.errorMessages = errorMessages;
    }

    public static BasicResponseDto addSuccess(String msg) {
        return new BasicResponseDto(HttpStatus.OK.value(), msg);
    }

    public static BasicResponseDto addBadRequest(String msg, List<String> errorMessages) {
        return new BasicResponseDto(HttpStatus.BAD_REQUEST.value(), msg, errorMessages);
    }

}
