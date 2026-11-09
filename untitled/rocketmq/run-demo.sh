#!/usr/bin/env bash
# 一键跑 RocketMQ demo：先起消费者，再发 100 条消息，最后统计
# 前提：Docker 里 rocketmq-namesrv / rocketmq-broker 已在运行
set -euo pipefail

cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-$HOME/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home}"
JAVA="$JAVA_HOME/bin/java"
LOG=/tmp/rmq-consumer.log

mvn -B -q compile
mvn -B -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
CP="target/classes:$(cat target/cp.txt)"

rm -f "$LOG"
echo "启动消费者..."
nohup "$JAVA" -cp "$CP" com.sys.Consumer > "$LOG" 2>&1 &
CONSUMER_PID=$!

for _ in $(seq 1 30); do
  grep -q "Consumer Started" "$LOG" 2>/dev/null && break
  sleep 1
done
echo "消费者就绪，开始发送..."

"$JAVA" -cp "$CP" com.sys.SyncProducer > /tmp/rmq-producer.log 2>&1
echo "发送完成: $(grep -c SEND_OK /tmp/rmq-producer.log) 条 SEND_OK"

# 新消费者组首次 rebalance 需要约 20s，等够再统计
echo "等待 rebalance（约 25s）..."
sleep 25
echo "消费收到: $(grep -c 'Received Message' "$LOG") 条"

kill "$CONSUMER_PID" 2>/dev/null || true
echo "日志: $LOG / /tmp/rmq-producer.log"
