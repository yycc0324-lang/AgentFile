package com.sys;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

public class Consumer {
    public static void main(String[] args) throws Exception {
        // 1. 实例化消费者，指定消费者组名
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("please_rename_unique_group_name");
        // 2. 指定 NameServer 地址
        consumer.setNamesrvAddr("localhost:9876");
        // 3. 订阅一个或多个 Topic，以及 Tag 来过滤需要消费的消息
        // 表达式说明：
        //   * 表示所有 Tag
        //   "TagA || TagC" 表示 TagA 或 TagC
        consumer.subscribe("TopicTest", "*");

        // 4. 注册回调实现类来处理从 Broker 拉取回来的消息
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                            ConsumeConcurrentlyContext context) {
                // 业务处理逻辑
                for (MessageExt msg : msgs) {
                    System.out.printf("Received Message: %s %n", new String(msg.getBody()));
                }
                // 返回消费状态：成功（CONSUME_SUCCESS）或 稍后重试（RECONSUME_LATER）
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        // 5. 启动消费者实例
        consumer.start();
        System.out.printf("Consumer Started.%n");
    }
}
