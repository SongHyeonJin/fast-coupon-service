package com.example.fastcoupon.entity;

import com.example.fastcoupon.entity.base.Timestamped;
import com.example.fastcoupon.enums.UserRoleEnum;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// SecurityContext(UserDetailsImpl)가 Redis 세션에 JDK 직렬화로 저장되므로 Serializable 필요
public class User extends Timestamped implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String tel;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRoleEnum role;

    @Builder
    public User(Long id, String name, String email, String password, String tel, UserRoleEnum role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.tel = tel;
        this.role = role;
    }

}
