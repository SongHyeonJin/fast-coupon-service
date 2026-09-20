package com.example.fastcoupon;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// test 프로필이 없으면 운영용 DB에 붙고, 같은 Kafka 그룹의 컨슈머가 다른 테스트의 메시지를 가로챈다
@SpringBootTest
@ActiveProfiles("test")
class FastcouponApplicationTests {

	@Test
	void contextLoads() {
	}

}
