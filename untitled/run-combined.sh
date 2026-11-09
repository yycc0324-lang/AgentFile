#!/usr/bin/env bash
# 一键跑"RocketMQ -> RabbitMQ"桥接全链路
# 前提：Docker 里 rocketmq-namesrv / rocketmq-broker / agents-rabbitmq 都在运行
set -euo pipefail
cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-$HOME/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home}"
JAVA="$JAVA_HOME/bin/java"
BRIDGE_LOG=/tmp/bridge.log
RABBIT_LOG=/tmp/rabbit-consumer.log
PRODUCER_LOG=/tmp/rmq-producer.log

mvn -B -q compile
mvn -B -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
CP="target/classes:$(cat target/cp.txt)"

rm -f "$BRIDGE_LOG" "$RABBIT_LOG" "$PRODUCER_LOG"

echo "[1/4] 启动 RabbitMQ 消费者..."
nohup "$JAVA" -cp "$CP" com.sys.rabbit.RabbitConsumer > "$RABBIT_LOG" 2>&1 &
RABBIT_PID=$!

echo "[2/4] 启动 RocketMQ -> RabbitMQ 桥接..."
nohup "$JAVA" -cp "$CP" com.sys.bridge.RocketMqToRabbitBridge > "$BRIDGE_LOG" 2>&1 &
BRIDGE_PID=$!

trap 'kill $RABBIT_PID $BRIDGE_PID 2>/dev/null || true' EXIT

for _ in $(seq 1 30); do
  grep -q "Consumer Started" "$RABBIT_LOG" 2>/dev/null && \
  grep -q "Bridge Started" "$BRIDGE_LOG" 2>/dev/null && break
  sleep 1
done
echo "      两端就绪"

# 新消费者组第一次 rebalance 需要约 20s
echo "[3/4] 等待 rebalance（25s）..."
sleep 25

echo "[4/4] 生产者往 RocketMQ 发 100 条..."
"$JAVA" -cp "$CP" com.sys.SyncProducer > "$PRODUCER_LOG" 2>&1
echo "      SendResult SEND_OK: $(grep -c SEND_OK "$PRODUCER_LOG") 条"

sleep 10
# 等桥接把消息转发完（路由刷新 + 拉取都有延迟，最多等 60s）
for _ in $(seq 1 60); do
  forwarded=$(grep -c 'Forwarded' "$BRIDGE_LOG" 2>/dev/null || true)
  [ "${forwarded:-0}" -ge 100 ] && break
  sleep 1
done
echo
echo "===== 结果 ====="
echo "桥接转发:  $(grep -c 'Forwarded' "$BRIDGE_LOG") 条"
echo "Rabbit 消费: $(grep -c 'Received Message' "$RABBIT_LOG") 条"
echo "队列堆积:  $(docker exec agents-rabbitmq rabbitmqctl -q list_queues name messages messages_unacknowledged 2>/dev/null | tail -1)"
echo
echo "日志: $BRIDGE_LOG / $RABBIT_LOG / $PRODUCER_LOG"
