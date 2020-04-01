package com.spring.amqp.demo.service;

import com.spring.amqp.demo.entity.User;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitMqSend {

    private  Message correctDataMessage;

    @Autowired
    RabbitTemplate rabbitTemplate;

    public void sendMesg(){
        //设置消息序列化转换器
     rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
     CorrelationData correlationData=new CorrelationData();
     //发送到哪一个交换器上
     rabbitTemplate.setExchange("test_exchange");
     //设置消息的路由键
     rabbitTemplate.setRoutingKey("test_route_haha");
        User user=new User();
        user.setUserID("1");
        user.setUserName("haha");
        /**
         *从 version 1.6.7 开始，引入了CorrelationAwareMessagePostProcessor接口，允许在转换消息后修改相关数据：
         * Message postProcessMessage(Message message, Correlation correlation);
         *
         * 在 version 2.0 中，不推荐使用此接口;该方法已移至MessagePostProcessor，
         * 默认 implementation 委托给postProcessMessage(Message message)。
         *
         * 该接口的实现类可以在转换消息后修改消息属性
         */
    rabbitTemplate.convertAndSend(user, new MessagePostProcessor() {
        @Override
        public Message postProcessMessage(Message message) throws AmqpException {
            MessageProperties messageProperties=message.getMessageProperties();
            //设置消息持久化
            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            /**
             * 之前的版本中需要设置class类属性，以便在序列化时进行转换
             * 新版本中不需要设置消息转换，spring会自动推断
             *设置消息头，便于消费者在接收时可以直接用该类进行接收
             messageProperties.setHeader(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME, KillSuccessUserInfo.class);
             */
            //提供一个类型映射，以便消费者可以直接接收到相同类型的消息
            messageProperties.setHeader(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME,User.class);
            //单位毫秒
            messageProperties.setExpiration("1000000");

            return message;
        }
    },correlationData);
    }

}
