package com.example.fastcoupon.security;

import com.example.fastcoupon.entity.User;
import com.example.fastcoupon.enums.UserRoleEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class UserDetailsImplSerializationTest {

    @DisplayName("Redis 세션(JDK 직렬화)에 저장되는 UserDetailsImpl은 직렬화/역직렬화가 가능해야 한다")
    @Test
    void userDetailsIsSerializable() throws Exception {
        User user = User.builder().id(1L).email("a@test.com").role(UserRoleEnum.USER).build();
        UserDetailsImpl original = new UserDetailsImpl(user, user.getEmail());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            UserDetailsImpl restored = (UserDetailsImpl) in.readObject();
            assertThat(restored.getUser().getId()).isEqualTo(1L);
            assertThat(restored.getUser().getRole()).isEqualTo(UserRoleEnum.USER);
        }
    }
}
