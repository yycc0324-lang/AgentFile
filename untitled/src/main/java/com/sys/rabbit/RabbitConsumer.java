package com.sys.rabbit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

/**
 * RabbitMQ 消费者：对应 RocketMQ 的 Consumer。
 * 会一直阻塞等在队列上，Ctrl+C 结束。
 */
public class RabbitConsumer {

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitConfig.HOST);
        factory.setPort(RabbitConfig.PORT);
        factory.setUsername(RabbitConfig.USERNAME);
        factory.setPassword(RabbitConfig.PASSWORD);

        Connection connection = factory.newConnection("rabbit-consumer");
        Channel channel = connection.createChannel();
        channel.queueDeclare(RabbitConfig.QUEUE, true, false, false, null);

        // 预取 10 条，处理完再拿下一批，避免消息全堆在一个消费者内存里
        channel.basicQos(10);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.printf("Received Message: %s%n", body);
            // 手动 ack：处理成功才确认，失败可以不 ack 让它重新入队
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        // autoAck = false，配合上面的手动 ack
        channel.basicConsume(RabbitConfig.QUEUE, false, deliverCallback, consumerTag -> {
        });

        System.out.println("Consumer Started.");
        // 阻塞主线程，让 JVM 一直活着
        new CountDownLatch(1).await();
    }
}
