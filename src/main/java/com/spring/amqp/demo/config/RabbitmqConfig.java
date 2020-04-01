package com.spring.amqp.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * rabbitmq 配置类用于创建交换器队列以及进行绑定
 */
@Configuration
public class RabbitmqConfig {

  private static final Logger logger= LoggerFactory.getLogger(RabbitmqConfig.class);

  
    @Autowired
    Environment env;

  
    @Autowired
    CachingConnectionFactory connectionFactory;


  // 消费者连接工厂配置
  @Autowired
  private SimpleRabbitListenerContainerFactoryConfigurer factoryConfigurer;


  @Bean(name="singleListenerContainer")
  public SimpleRabbitListenerContainerFactory listenerContainerFactory(){

    SimpleRabbitListenerContainerFactory factory=new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(new Jackson2JsonMessageConverter());
    //为了加快消费队列中的消息，可以使用多线程来起多个相同的消费者进行并行消费，加快队列中的消息消费，此属性代表每次只有一个消费者进行消费
    factory.setConcurrentConsumers(1);
    //当同时配置了concurrency和max-concurrency属性后，那么代表正常情况下有5个 如果消费者很繁忙，那么会增加新的相同消费者，最多不超过15个
    factory.setMaxConcurrentConsumers(1);
    factory.setPrefetchCount(2);
   // factory.setBatchSize(1);
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

    return factory;
  }

  @Bean(name="multiListenerContainer")
  public SimpleRabbitListenerContainerFactory multiContainerFactory(){
    SimpleRabbitListenerContainerFactory factory=new SimpleRabbitListenerContainerFactory();
    factoryConfigurer.configure(factory,connectionFactory);
    factory.setMessageConverter(new Jackson2JsonMessageConverter());
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    factory.setConcurrentConsumers(env.getProperty("spring.rabbitmq.listener.simple.concurrency",int.class));
    factory.setMaxConcurrentConsumers(env.getProperty("spring.rabbitmq.listener.simple.max-concurrency",int.class));
    factory.setPrefetchCount(env.getProperty("spring.rabbitmq.listener.simple.prefetch",int.class));
    return  factory;
  }
    @Bean
    public Queue testQueue(){
        return new Queue("test_queue",true,false,false,null);
    }

    @Bean
    public TopicExchange testTopicExchange(){
        return new TopicExchange("test_exchange",true,false,null);
    }

    @Bean
    public Binding testBinding(){
    return BindingBuilder.bind(testQueue()).to(testTopicExchange()).with("test_route_haha");
    }

  @Bean
  public RabbitTemplate rabbitTemplate(){
    connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
    connectionFactory.setPublisherReturns(true);
    RabbitTemplate rabbitTemplate=new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMandatory(true);
    rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
      @Override
      public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        logger.info("消息发送失败，根据routingKey 未找到正确的队列:----"+message.toString());
      }
    });

    rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
      @Override
      public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        /**
         * 注意如果消息成功发送到了
         */
        if(ack){
          //如果正确的发送到了
          logger.info("消息已正确发送到mq中:"+correlationData);
        }else {
          logger.info("因为mq自身原因，消息丢失了");
          logger.info("丢失的消息为:"+correlationData);
        }
      }
    });
    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    return rabbitTemplate;
    }
}
