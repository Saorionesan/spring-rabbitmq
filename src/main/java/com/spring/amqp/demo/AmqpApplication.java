package com.spring.amqp.demo;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication

public class AmqpApplication {

	public static void main(String[] args) {
		SpringApplication.run(AmqpApplication.class, args);
	}

}
