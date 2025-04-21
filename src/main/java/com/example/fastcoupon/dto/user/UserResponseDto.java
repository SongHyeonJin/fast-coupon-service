package com.example.fastcoupon.dto.user;

import lombok.Getter;

@Getter
public class UserResponseDto {

    private String name;
    private String msg;

    public UserResponseDto(String name, String msg) {
        this.name = name;
        this.msg = msg;
    }

}

