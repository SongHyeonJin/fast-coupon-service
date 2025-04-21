package com.example.fastcoupon.controller;

import com.example.fastcoupon.aop.LoginSessionInject;
import com.example.fastcoupon.dto.user.LoginRequestDto;
import com.example.fastcoupon.dto.user.SignupRequestDto;
import com.example.fastcoupon.dto.user.UserResponseDto;
import com.example.fastcoupon.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signup(@Valid @RequestBody SignupRequestDto requestDto) {
        return ResponseEntity.ok( userService.signup(requestDto));
    }

    @LoginSessionInject
    @PostMapping("/login")
    public ResponseEntity<UserResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto) {
        return ResponseEntity.ok(userService.login(requestDto));
    }

}
