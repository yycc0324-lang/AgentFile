package com.sys.rabbit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

/**
 * RabbitMQ 生产者：往队列里发 100 条消息，对应 RocketMQ 的 SyncProducer。
 */
public class RabbitProducer {

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitConfig.HOST);
        factory.setPort(RabbitConfig.PORT);
        factory.setUsername(RabbitConfig.USERNAME);
        factory.setPassword(RabbitConfig.PASSWORD);

        // try-with-resources：退出时自动关闭 channel 和 connection
        try (Connection connection = factory.newConnection("rabbit-producer");
             Channel channel = connection.createChannel()) {

            // 声明队列：持久化、非排他、不自动删除
            channel.queueDeclare(RabbitConfig.QUEUE, true, false, false, null);

            for (int i = 0; i < 100; i++) {
                String body = "Hello RabbitMQ " + i;
                // 默认交换机（""）+ 队列名作为 routing key = 直投队列
                channel.basicPublish("", RabbitConfig.QUEUE, null,
                        body.getBytes(StandardCharsets.UTF_8));
                System.out.printf("Sent: %s%n", body);
            }

            System.out.println("RabbitMQ 发送完成：100 条");
        }
    }
}
