package com.spring.amqp.demo;

import com.spring.amqp.demo.service.RabbitMqRecevie2;
import com.spring.amqp.demo.service.RabbitMqRecive;
import com.spring.amqp.demo.service.RabbitMqSend;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AmqpApplicationTests {

	@Autowired
	RabbitMqSend send;
	@Autowired
	RabbitMqRecive recevie;
	@Test
	void contextLoads() {
		send.sendMesg();
	}

}
