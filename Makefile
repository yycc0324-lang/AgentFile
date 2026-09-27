# AgentForge 本地开发常用命令
# 所有目标都在仓库根目录执行，依赖根目录的 docker-compose.yml 与 .env。

SHELL := /bin/bash
COMPOSE := docker compose

.DEFAULT_GOAL := help
.PHONY: help env config up down restart ps logs clean health urls mysql-shell redis-cli

help: ## 显示所有可用命令
	@echo "AgentForge 开发命令："
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}'

env: ## 若 .env 不存在则从 .env.example 生成
	@if [ -f .env ]; then \
		echo ".env 已存在，跳过（如需重建请先手动删除）"; \
	else \
		cp .env.example .env && echo "已生成 .env，请按需修改密钥"; \
	fi

config: ## 校验 compose 文件语法与变量插值结果
	@$(COMPOSE) config --quiet && echo "docker-compose.yml 校验通过"

up: env ## 启动全部基础设施（后台运行）
	@$(COMPOSE) up -d
	@$(MAKE) --no-print-directory urls

down: ## 停止并删除容器（保留数据卷）
	@$(COMPOSE) down

restart: ## 重启全部服务
	@$(COMPOSE) restart

ps: ## 查看服务状态
	@$(COMPOSE) ps

logs: ## 跟踪查看日志（可加服务名：make logs mysql）
	@$(COMPOSE) logs -f --tail=100 $(filter-out $@,$(MAKECMDGOALS))

clean: ## 停止并删除容器 + 数据卷（会清空所有本地数据，需输入 y 确认）
	@read -p "将删除全部数据卷，确认？[y/N] " ans; \
	if [ "$$ans" = "y" ] || [ "$$ans" = "Y" ]; then \
		$(COMPOSE) down -v && echo "已清理容器与数据卷"; \
	else \
		echo "已取消"; \
	fi

health: ## 查看各容器健康状态
	@$(COMPOSE) ps --format 'table {{.Name}}\t{{.Service}}\t{{.Status}}'

urls: ## 打印各服务访问地址
	@set -a; if [ -f .env ]; then . ./.env; fi; set +a; \
	echo ""; \
	echo "  MySQL        127.0.0.1:$${MYSQL_PORT:-3306}   (库 $${MYSQL_DATABASE:-agentforge})"; \
	echo "  Redis        127.0.0.1:$${REDIS_PORT:-6379}"; \
	echo "  MinIO API    127.0.0.1:$${MINIO_API_PORT:-9000}"; \
	echo "  MinIO 控制台 http://localhost:$${MINIO_CONSOLE_PORT:-9001}"; \
	echo "  Qdrant HTTP  127.0.0.1:$${QDRANT_HTTP_PORT:-6333}   (Dashboard: /dashboard)"; \
	echo "  Qdrant gRPC  127.0.0.1:$${QDRANT_GRPC_PORT:-6334}"; \
	echo ""

mysql-shell: ## 进入 MySQL 交互式客户端
	@$(COMPOSE) exec mysql sh -c 'exec mysql -uroot -p"$$MYSQL_ROOT_PASSWORD" "$$MYSQL_DATABASE"'

redis-cli: ## 进入 Redis 交互式客户端
	@$(COMPOSE) exec redis sh -c 'if [ -n "$$REDIS_PASSWORD" ]; then exec redis-cli --no-auth-warning -a "$$REDIS_PASSWORD"; else exec redis-cli; fi'

# 允许 `make logs mysql` 这种带参数用法而不报 "No rule to make target"
%:
	@:
