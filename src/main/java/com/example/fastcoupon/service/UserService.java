package com.example.fastcoupon.service;

import com.example.fastcoupon.dto.user.SignupRequestDto;
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
    public void signup(SignupRequestDto requestDto) {
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
    }

}
