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

    //spring 环境变量 可以获取到properties属性值
    @Autowired
    Environment env;

  /**
   *用于管理客户端和RabbitMQ broker连接的是ConnectionFactory接口。该接口的实现类用于提供org.springframework.amqp.rabbit.connection.Connection的实例。
   * 该连接实例为com.rabbitmq.client.Connection的包装类。对于ConnectionFactory接口，spring提供了唯一实现CachingConnectionFactory。
   * 创建该实例时可以使用构造器来进行属性注入.如果要配置连接的缓存的channel数目 可以调用setChannelCacheSize()方法
   *channelCacheSize 默认值为25
   *
   * 如果观察到channel被频繁的关闭和开启，需要调高channelCacheSize的值
   *
   *如果要缓存连接（即使用多个连接进行信息传递），注意在此类连接上创建的 Channels 也会被缓存。需要将cacheMode设置为CacheMode.CONNECTION。默认为CacheMode.CHANNEL
   * 同样可以调用setConnectionCacheSize()方法设置缓存的空闲连接数。设置为这种情况下，不支持自动创建队列等
   * 注意：这不限制连接数，它指定允许多少 idle 打开连接。（与nginx中的keepalive参数类似）
   * 如果要限制连接总数,需要设置connectionLimit属性。
   */
    @Autowired
    CachingConnectionFactory connectionFactory;


  // 消费者连接工厂配置
  @Autowired
  private SimpleRabbitListenerContainerFactoryConfigurer factoryConfigurer;

  /**
   * 单一消费者实例
   * 可以配置多个消费者容器工厂 这些容器工厂用来在消费消息时在后台创建消息 listener 容器。
   */
  @Bean(name="singleListenerContainer")
  public SimpleRabbitListenerContainerFactory listenerContainerFactory(){
/**
 *  使用RabbitListenerContainerFactory为每个带注释的方法在后台创建消息 listener 容器。
 *  此处SimpleRabbitListenerContainerFactory 为其实现类
 */
    SimpleRabbitListenerContainerFactory factory=new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    /**
     更改消息转换器，只需将其作为 property 添加到容器工厂 bean
     设置对应的消息转换器
     这将配置一个 Jackson2 转换器，该转换器需要存在标题信息以指导转换
     即在发送消息时设置的消息标题属性
     */
    factory.setMessageConverter(new Jackson2JsonMessageConverter());
    //为了加快消费队列中的消息，可以使用多线程来起多个相同的消费者进行并行消费，加快队列中的消息消费，此属性代表每次只有一个消费者进行消费
    factory.setConcurrentConsumers(1);
    //当同时配置了concurrency和max-concurrency属性后，那么代表正常情况下有5个 如果消费者很繁忙，那么会增加新的相同消费者，最多不超过15个
    factory.setMaxConcurrentConsumers(1);
    //每次预拉取消息数为1 表示消费者端最多保持1个未确认的消息
    //即相当于Java客户端中的 channel.basicQos(64)方法，
    factory.setPrefetchCount(2);
   // factory.setBatchSize(1);
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

    return factory;
  }
  /**
   *多个消费者示例
   * 使用的都是同一个消费者工厂
   * 但是bean的名称不一样
   */
  @Bean(name="multiListenerContainer")
  public SimpleRabbitListenerContainerFactory multiContainerFactory(){
    SimpleRabbitListenerContainerFactory factory=new SimpleRabbitListenerContainerFactory();
    factoryConfigurer.configure(factory,connectionFactory);
    factory.setMessageConverter(new Jackson2JsonMessageConverter());

    /**
     * 消费者确认（消费者应答），当消费者收到消息处理完毕后给rabbitmq发送一个ack确认
     * NONE：不发送任何确认（ack）
     * MANUAL listener 必须通过调用Channel.basicAck()来确认所有消息
     *AUTO 容器会自动确认消息
     * 默认情况下消息消费者是自动 ack （确认）消息的
     */
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    //设置多少消费者用于进行消息消费
    factory.setConcurrentConsumers(env.getProperty("spring.rabbitmq.listener.simple.concurrency",int.class));
    factory.setMaxConcurrentConsumers(env.getProperty("spring.rabbitmq.listener.simple.max-concurrency",int.class));
    /**
     * 预拉取多少消息，即消费者端最多能保存的未确认消费数。如果超过这个数rabbitmq还未收到确认，那么mq将不会再发送消息给消费者，直到收到了确认
     *
     * 如果 AcknowledgeMode.NONE 那么忽略该属性，即使该属性设置值也无效
     */
    factory.setPrefetchCount(env.getProperty("spring.rabbitmq.listener.simple.prefetch",int.class));
    return  factory;
  }
    @Bean
    public Queue testQueue(){
        /**
         *springframework.amqp.core.Queue 是对spring对于amqp中queue部分的抽象,其相对于普通Java客户端中的queueDeclare方法
         * 参数详解:
         * name: 要声明的队列名称、durable：队列是否持久化。持久化的队列会存盘，在服务器重启的时候可以保证不丢失相关信息。
         * exclusive：设置是否排他。为 true 则设置队列为排他的。如果一个队列被声明为排他队列，该队列仅对首次声明它的连接可见，并在连接断开时自动删除。
         * autoDelete:设置是否自动删除。为 true 则设置队列为自动删除。自动删除的前提是:至少有一个消费者连接到这个队列，之后所有与这个队列连接的消费者都断开时，才会自动删除。
         * arguments：设置队列的其他一些参数，如 x-rnessage-ttl x-expires x-rnax-length x-rnax-length-bytes x-dead-letter-exchange x-dead-letter-routing-key, x-max-priority
         * 设置死信交换机或者死信路由等其他参数
         */
        return new Queue("test_queue",true,false,false,null);
    }

    @Bean
    public TopicExchange testTopicExchange(){
        /**
         * 相对于Java客户端中的exchangeDeclare 方法 此处的type直接默认为topic。spring中除了TopicExchange类之外还有DirectExchage、FanoutExchange
         * 参数详解:
         * name: 交换器名称
         * durable:是否持久化。设置持久化后，mq在重启后不会丢失相关信息
         * autoDelete：是否自动删除。autoDelete 设置为 true 表示自动删除。自动删除的前提是至少有一个队列或者交换器与这个交换器绑定，之后所有与这个交换器绑定的队列或者交换器都与解绑。
         * arguments：其他一些结构化参数，比如 alternate-exchange 该参数需要一个map
         * 再实例化完成后可以调用该对象进行一些其他的设置。比如设置是否为内置队列.
         */
        return new TopicExchange("test_exchange",true,false,null);
    }

    @Bean//将交换器和队列通过路由键（binding key）来绑定
    //可以直接new Binding() 进行绑定,也可以使用BindingBuilder来进行绑定
    public Binding testBinding(){
        //将该队列使用路由键与对应交换器绑定
    return BindingBuilder.bind(testQueue()).to(testTopicExchange()).with("test_route_haha");
    }

  /**
   * RabbitTemplate 为AmqpTemplate 接口的实现类
   * Spring AMQP 提供了一个起着核心作用的“模板”。定义主要操作的接口称为AmqpTemplate
   *
   * @return
   */
  @Bean
  public RabbitTemplate rabbitTemplate(){
    /**
     * 设置生产者确认
     * 确保消息被正确发送到mq中
     * 注意该方法已过时 新版中需要一个CachingConnectionFactory实例，将其publisherConfirm属性设置为ConfirmType.CORRELATED
     */
    connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
    connectionFactory.setPublisherReturns(true);
    RabbitTemplate rabbitTemplate=new RabbitTemplate(connectionFactory);
    /**
     * Mandatory 该参数为channel.basicPublish方法中的参数
     * 当mandatory 参数设为 true 时，交换器无法根据自身的类型和路由键找到一个符合条件
     的队列，那么 RabbitMq会调用 Basic.Return 命令将消息返回给生产者 。当 mandatory
     数设置为 false 时，出现上述情形，则消息直接被丢弃
     *
     *获取到返回的消息通过调用channel addReturnListener 来添加 ReturnListener 监昕器实现。
     */
    rabbitTemplate.setMandatory(true);

    /**
     * 如果消息发送到路由器中但无法找到一个合适的队列，那么rabbitmq会将其返回给生产者
     * 此时需要调用setReturnCallback(ReturnCallback callback)注册RabbitTemplate.ReturnCallback，返回发送到 client。
     * 开启此功能需要将setPublisherReturns 设置为true
     *
     */
    rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
      //当消息未正确的路由到队列时，其会调用该方法
      /**
       * 该方法对应于客户端中的channel.addReturnListener(new ReturnListener(){})
       * @param message 返回的消息 可以在此处进行重新发送等
       * @param replyCode 返回的状态码
       * @param replyText
       * @param exchange  路由
       * @param routingKey 消息路由键
       */
      @Override
      public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        logger.info("消息发送失败，根据routingKey 未找到正确的队列:----"+message.toString());
      }
    });

    /**
     * 发布者确认 一旦消息被投递到所有匹配的队列之后，RabbitMQ 会发送一个确认 （Basic.Ack) 给生产者(包含消息的唯一 ID) ，这就使得生产者知晓消息已经正确到达了目的地了。
     * 如果消息和队列是持久化的，那么ack会在消息落盘之后发出
     *
     *该confirm 对应客户端中的 channel.confirmSelect() 将信道置为发送方确认的模式.
     * 在spring中可以直接调用RabbitTemplate.ConfirmCallback发送确认给客户端setConfirmCallback(ConfirmCallback callback)。
     */
    rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
      /**
       * 如果消息被正确的发送到了rabbitmq 那么，其会调用该方法，并且ack为true
       * 如果消息因为rabbitmq自身原因导致丢失，那么ack会为false 即相当于为该消息被标注了nack
       * 注意这和上面的不同，上面的return是因为没有合适的队列导致消息被返回(但是已经被正确发送到MQ 中)
       * 如果发送到一个不存在的交换机中，那么也会调用下面的方法，并且ack为false
       * 一旦消息成功发送到mq中，不管其是否正确路由到队列中，其都会返回一个ack并且为true
       * @param correlationData  注意这个CorrelationData参数需要在消息发送时进行设置，如果在调用convertAndSend方法时未进行设置
       * correlationData 属性值，那么此处收到的将为null,可以将此处设置为消息的ID号以保证在消息没有发送到mq中时进行纠错
       * @param ack 消息是否被正确发送mq中 为true正常发送 为false 发送失败
       * @param cause 引起丢失的异常原因
       *  具体见 https://www.cnblogs.com/wangiqngpei557/p/9381478.html
       */
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
    /**
     * 设置消息转换器 实现MessageConverter接口，不配置默认实现为SimpleMessageConverter，该转换器会将text类型转换为string
     * 如果配置了Jackson2JsonMessageConverter 转换器，以前版本需要在发送消息时配置消息的header属性，现在不需要。spring会根据消息
     * 类型来进行默认推断
     *
     * 如果此处设置了消息转换器，为了消费者能够正常接收，其也需要在容器工厂中配置相同的消息转换器
     */
    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    return rabbitTemplate;
    }
}
