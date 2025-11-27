package com.nhom10.coolmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class CoolmateApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoolmateApplication.class, args);
	}

}
