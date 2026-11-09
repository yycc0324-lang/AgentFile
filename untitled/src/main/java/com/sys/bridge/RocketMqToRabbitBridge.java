package com.sys.bridge;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.sys.rabbit.RabbitConfig;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

/**
 * 桥接器：订阅 RocketMQ 的 TopicTest，把消息原样转发到 RabbitMQ 队列。
 *
 * 数据流：SyncProducer -> RocketMQ(TopicTest) -> 本类 -> RabbitMQ(rocketmq-bridge-queue) -> RabbitConsumer
 *
 * 这是"两个 MQ 结合"最典型的落地形态之一：
 * 上游系统用的是 RocketMQ，下游系统只认 RabbitMQ，中间加一层桥接做协议转换。
 */
public class RocketMqToRabbitBridge {

    public static void main(String[] args) throws Exception {
        // ---------- 1. 连 RabbitMQ ----------
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitConfig.HOST);
        factory.setPort(RabbitConfig.PORT);
        factory.setUsername(RabbitConfig.USERNAME);
        factory.setPassword(RabbitConfig.PASSWORD);

        Connection rabbitConnection = factory.newConnection("rocketmq-to-rabbit-bridge");
        Channel rabbitChannel = rabbitConnection.createChannel();
        rabbitChannel.queueDeclare(RabbitConfig.QUEUE, true, false, false, null);
        // 开启发布确认，保证转发不丢消息（性能会略有下降，演示够用）
        rabbitChannel.confirmSelect();

        // ---------- 2. 连 RocketMQ ----------
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("bridge_group");
        consumer.setNamesrvAddr("localhost:9876");
        consumer.subscribe("TopicTest", "*");

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                try {
                    // amqp 的 Channel 不是线程安全的，而 RocketMQ 回调是多线程的，
                    // 所以必须串行化写入，否则会出现帧错乱
                    synchronized (rabbitChannel) {
                        rabbitChannel.basicPublish("", RabbitConfig.QUEUE, null, msg.getBody());
                    }
                    System.out.printf("Forwarded: %s%n", body);
                } catch (Exception e) {
                    System.err.printf("转发失败，稍后重试: %s (%s)%n", body, e.getMessage());
                    // 让 RocketMQ 重投这批消息
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.start();
        System.out.printf("Bridge Started. RocketMQ[TopicTest] --> RabbitMQ[%s]%n", RabbitConfig.QUEUE);

        // 阻塞主线程
        new CountDownLatch(1).await();
    }
}
