package com.spring.amqp.demo.service;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.spring.amqp.demo.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class RabbitMqRecive {

    private final  static Logger logger= LoggerFactory.getLogger(RabbitMqRecive.class);

    /**
     * @Header 获取消息中的消息头相关属性
     * @Payload 获取消息中的主体属性
     * @param user
     * @param delivery_tag
     */
    @RabbitListener(queues = "test_queue",containerFactory = "singleListenerContainer")
    public void getUser(@Payload User user, @Header(AmqpHeaders.DELIVERY_TAG) long delivery_tag, @Headers Map<String,Object> map,Channel channel){
            logger.info("--已接收到消息--"+user.getUserName());
            logger.info("当前接收到的消息tag为--"+delivery_tag);
        System.out.println(map);
        //消费者返回确认给已接受的消息
        /**
         * 如果为false 那么有多少消息未确认那么就调用多少次确认方法
         * 如果未true 则一次性确认 deliveryTag 编号之前所有未被当前消费者确认的消息。
         */
        try {
            channel.basicAck(delivery_tag,true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
