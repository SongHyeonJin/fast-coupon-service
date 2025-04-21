package com.example.fastcoupon.dto.user;

import lombok.Getter;

@Getter
public class UserResponseDto {

    private Long userId;
    private String name;
    private String msg;

    public UserResponseDto(Long userId, String name, String msg) {
        this.userId = userId;
        this.name = name;
        this.msg = msg;
    }
}

