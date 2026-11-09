# RocketMQ + RabbitMQ 联合 Demo

同一套代码里跑两种 MQ，并演示两者之间的桥接。全部跑在 Docker 里，代码用原生客户端（不走 Spring）。

## 数据流

```
SyncProducer ──> RocketMQ (TopicTest, 9876/10911)
                        │
                        │ RocketMqToRabbitBridge 订阅 TopicTest
                        ▼
                 RabbitMQ (rocketmq-bridge-queue, 5672)
                        │
                        ▼
                  RabbitConsumer
```

## 代码结构

| 类 | 作用 |
|---|---|
| `com.sys.SyncProducer` | RocketMQ 生产者，同步发 100 条到 `TopicTest` |
| `com.sys.Consumer` | RocketMQ 消费者，订阅 `TopicTest` 的 `*` |
| `com.sys.rabbit.RabbitProducer` | RabbitMQ 生产者，发 100 条到 `rocketmq-bridge-queue` |
| `com.sys.rabbit.RabbitConsumer` | RabbitMQ 消费者，手动 ack |
| `com.sys.rabbit.RabbitConfig` | 连接参数与队列名常量 |
| `com.sys.bridge.RocketMqToRabbitBridge` | 桥接：RocketMQ 消费 → RabbitMQ 转发 |

## 依赖的容器

| 容器 | 镜像 | 端口 |
|---|---|---|
| `rocketmq-namesrv` | apache/rocketmq:4.9.4 | 9876 |
| `rocketmq-broker` | apache/rocketmq:4.9.4 | 10909/10911/10912 |
| `agents-rabbitmq` | rabbitmq:3.13-management | 5672 / 15672（管理台） |

配置见 `rocketmq/broker.conf`。关键两项：`brokerIP1=127.0.0.1`（客户端在宿主机）、`autoCreateTopicEnable=true`（免手动建 Topic）。

## 三种跑法

```bash
# 1) 只跑 RocketMQ 收发
./rocketmq/run-demo.sh

# 2) 只跑 RabbitMQ 收发
mvn -q compile
java -cp "target/classes:$(cat target/cp.txt)" com.sys.rabbit.RabbitProducer
java -cp "target/classes:$(cat target/cp.txt)" com.sys.rabbit.RabbitConsumer   # 阻塞，Ctrl+C 退出

# 3) 全链路：RocketMQ -> 桥接 -> RabbitMQ
./run-combined.sh
```

## 运行截图

| 内容 | 文件 |
|---|---|
| 1. 启动 RocketMQ / RabbitMQ 容器（docker run） | `docs/screenshot-1-startup.png` |
| 2. 全链路运行结果（100 条收发成功） | `docs/screenshot-2-run.png` |
| 3. IDEA 里运行 SyncProducer 的 Run 控制台 | `docs/idea-run-producer.png` |
| 4. IDEA 里运行 Consumer 的 Run 控制台 | `docs/idea-run-consumer.png` |

IDEA 里同理：各 `main` 直接 Run。**先起消费者，再起生产者**，RabbitMQ 管理台 http://localhost:15672 看队列。

## 踩坑记录

1. **新消费者组首次 rebalance 约 20–25 秒**。消费者刚启动时 `TopicTest` 可能还不存在，要等路由发现和 rebalance 完成才开始拉消息。看到"没输出"先等半分钟。
2. **新消费者组会从头消费**。本次实测 `bridge_group` 首次启动时把历史上已有的 100 条也转发了一遍（offset 从 0 开始）。想只处理新消息，用新 Topic，或显式设置 `consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET)`。
3. **RabbitMQ 的 `Channel` 不是线程安全的**，而 RocketMQ 的回调是多线程的，桥接里必须 `synchronized (channel)` 或每线程一个 Channel。这是这类桥接最容易出的生产事故。
4. **`guest` 账号只能从 localhost 连**（RabbitMQ 的 loopback_users 限制）。
5. **amqp-client 5.21.0 只能配 slf4j-api 1.7.36**，绑 2.x 的 slf4j-simple 会报 `No SLF4J providers`。
6. RocketMQ 4.9.4 **没有 Web 控制台**，要看得另跑 rocketmq-dashboard；RabbitMQ 自带 15672。
