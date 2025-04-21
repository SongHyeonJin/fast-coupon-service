package com.example.fastcoupon.service;

import com.example.fastcoupon.dto.LoginRequestDto;
import com.example.fastcoupon.dto.user.SignupRequestDto;
import com.example.fastcoupon.dto.user.UserResponseDto;
import com.example.fastcoupon.entity.User;
import com.example.fastcoupon.enums.ExceptionEnum;
import com.example.fastcoupon.enums.UserRoleEnum;
import com.example.fastcoupon.exception.ErrorException;
import com.example.fastcoupon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDto signup(SignupRequestDto requestDto) {
        String email = requestDto.getEmail();
        String password = passwordEncoder.encode(requestDto.getPassword());
        String name = requestDto.getName();
        String tel = requestDto.getTel();

        Optional<User> checkEmail = userRepository.findByEmail(email);
        if (checkEmail.isPresent()) {
            throw new ErrorException(ExceptionEnum.EMAIL_DUPLICATION);
        }

        User user = User.builder()
                .email(email)
                .password(password)
                .name(name)
                .tel(tel)
                .role(UserRoleEnum.USER)
                .build();
        userRepository.save(user);
        return new UserResponseDto(user.getId(), user.getName(), "회원가입 성공");
    }

    @Transactional
    public UserResponseDto login(LoginRequestDto requestDto) {
        String email = requestDto.getEmail();
        String password = requestDto.getPassword();

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ErrorException(ExceptionEnum.USER_NOT_FOUND)
        );

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ErrorException(ExceptionEnum.WRONG_PASSWORD);
        }

        return new UserResponseDto(user.getId(), user.getName(), "로그인 성공");
    }

}
