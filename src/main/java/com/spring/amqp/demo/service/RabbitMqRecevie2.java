package com.spring.amqp.demo.service;

import com.spring.amqp.demo.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service

public class RabbitMqRecevie2 {
    private final  static Logger logger= LoggerFactory.getLogger(RabbitMqRecevie2.class);

  /*  @RabbitHandler
    public void listen(User user){
        logger.info("receive2接收到的用户消息为："+user.getUserName());
    }*/

}
