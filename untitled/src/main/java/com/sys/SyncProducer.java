package com.sys;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

public class SyncProducer {
    public static void main(String[] args) throws Exception {
        // 1. 实例化消息生产者 Producer，并指定生产者组名
        DefaultMQProducer producer = new DefaultMQProducer("please_rename_unique_group_name");
        // 2. 指定 NameServer 地址
        producer.setNamesrvAddr("localhost:9876");
        // 3. 启动 Producer 实例
        producer.start();

        for (int i = 0; i < 100; i++) {
            // 4. 创建消息，指定 Topic、Tag 和消息体
            Message msg = new Message("TopicTest" /* Topic */,
                    "TagA" /* Tag */,
                    ("Hello RocketMQ " + i).getBytes(RemotingHelper.DEFAULT_CHARSET) /* Message body */);
            // 5. 发送消息并获取发送结果（同步方式）
            SendResult sendResult = producer.send(msg);
            System.out.printf("%s%n", sendResult);
        }

        // 6. 如果不再发送消息，关闭 Producer 实例
        producer.shutdown();
    }
}
