package com.example.fastcoupon.dto.user;

import com.example.fastcoupon.enums.UserRoleEnum;
import lombok.Getter;

@Getter
public class UserResponseDto {

    private Long userId;
    private String name;
    private String email;
    private UserRoleEnum role;
    private String msg;

    public UserResponseDto(Long userId, String name, String email, UserRoleEnum role, String msg) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.msg = msg;
    }
}

