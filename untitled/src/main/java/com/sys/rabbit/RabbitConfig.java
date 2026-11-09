package com.sys.rabbit;

/**
 * RabbitMQ 连接与队列常量的统一出口，生产者 / 消费者 / 桥接共用。
 */
public final class RabbitConfig {

    public static final String HOST = "localhost";
    public static final int PORT = 5672;
    public static final String USERNAME = "guest";
    public static final String PASSWORD = "guest";

    /** RocketMQ 转发过来的消息落到这个队列 */
    public static final String QUEUE = "rocketmq-bridge-queue";

    private RabbitConfig() {
    }
}
