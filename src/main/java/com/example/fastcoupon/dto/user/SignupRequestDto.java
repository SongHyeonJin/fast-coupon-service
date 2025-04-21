package com.example.fastcoupon.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequestDto {

    @Pattern(regexp = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "유효한 이메일 주소를 입력해주세요.")
    @NotBlank(message = "E-mail을 입력해주세요.")
    private String email;

    @Size(min = 8, max = 15, message = "비밀번호는 최소 8자에서 15자 사이로만 가능합니다.")
    @Pattern(regexp = "(?=.*[a-zA-Z])(?=.*[0-9])(?=.*\\p{Punct}).+$", message = "비밀번호는 대소문자, 숫자, 특수문자를 포함해야됩니다.")
    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    @NotBlank(message = "성함을 입력해주세요.")
    private String name;

    @NotBlank(message = "전화번호를 입력해주세요.")
    private String tel;


}
