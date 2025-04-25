package com.example.fastcoupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FastcouponApplication {

	public static void main(String[] args) {
		SpringApplication.run(FastcouponApplication.class, args);
	}

}
