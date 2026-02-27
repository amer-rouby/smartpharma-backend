package com.smartpharma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartpharmaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartpharmaBackendApplication.class, args);
	}

}
