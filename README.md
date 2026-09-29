# AgentForge

一个 Monorepo：Java 平台服务 + Python Agent 运行时 + React 前端，配套 MySQL / Redis / MinIO / Qdrant 基础设施。

当前进度：**阶段 2 — platform-api 的 Spring Boot 骨架已就绪**（Maven + `com.agentforge.platform`，接通 MySQL，业务表由 Flyway 迁移脚本管理）。`agent-runtime`、`web` 两个服务尚未创建。

## 目录结构

```
agentforge/
├── docker-compose.yml        # 本地基础设施编排（MySQL/Redis/MinIO/Qdrant）
├── Makefile                  # 开发常用命令入口
├── .env.example              # 环境变量模板（复制为 .env 后使用）
├── infra/
│   └── mysql/init.sql        # MySQL 首次初始化脚本（库级初始化）
├── platform-api/             # Java 平台服务（Spring Boot 3.5 + Maven，包名 com.agentforge.platform）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/agentforge/platform/PlatformApiApplication.java
│       └── resources/
│           ├── application.yml            # 数据源、JPA、Flyway 配置
│           └── db/migration/V1__init_schema.sql   # 业务表结构（Flyway 执行）
├── agent-runtime/            # Python Agent 运行时（app/{agent,api,clients,rag,tools}）— 待实现
├── web/                      # 前端（React + TypeScript + Vite，src/{api,components,pages,types}）— 待实现
├── scripts/                  # 运维/开发脚本 — 待补充
└── data/                     # Agent 的 RAG 原始语料（blog/、repo/），已被 .gitignore 忽略
```

## 前置要求

| 依赖 | 建议版本 | 本机实测 |
| --- | --- | --- |
| Docker + Compose | Compose v2 及以上 | Docker 29.1.3 / Compose v2.40.3 |
| GNU Make | 任意 | macOS 自带 |

后续开发各服务时还需要：Java 21+ 与 Maven、Python 3.13+ 与 uv、Node 20+ 与 pnpm（本机均已安装）。

## 快速开始

```bash
cp .env.example .env    # 生成实际配置，按需修改密钥
make up                 # 启动全部基础设施（等价于 docker compose up -d）
make urls               # 查看各服务访问地址
make ps                 # 查看容器状态与健康检查
```

> 启动前请确认 Docker Desktop 已运行（`docker info` 能正常返回）。

## 服务与端口

| 服务 | 容器名 | 默认宿主端口 | 用途 |
| --- | --- | --- | --- |
| MySQL 8.4 | `agentforge-mysql` | 3306 | platform-api 业务元数据 |
| Redis 7.4 | `agentforge-redis` | 6379 | 缓存 / 会话 / 任务队列 |
| MinIO | `agentforge-minio` | 9000（S3 API）、9001（控制台） | 对象存储：文档、附件、模型产物 |
| Qdrant | `agentforge-qdrant` | 6333（HTTP）、6334（gRPC） | 向量库，供 agent-runtime RAG 检索 |

platform-api 本身是本地 Java 进程（不进容器）：默认监听 `8080`，健康检查 `http://localhost:8080/actuator/health`。

MinIO 控制台：<http://localhost:9001> ，账号密码取自 `.env` 的 `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`。
Qdrant Dashboard：<http://localhost:6333/dashboard> 。

## 环境变量

`.env` 由 `.env.example` 复制而来，**不要提交到仓库**（已在 `.gitignore` 中忽略）。

| 变量 | 说明 |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 |
| `MYSQL_DATABASE` | 首次初始化时自动创建的库名 |
| `REDIS_PASSWORD` | 留空表示不启用密码；填写后 Redis 会以 `--requirepass` 启动 |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | MinIO 账号与密码 |
| `QDRANT_API_KEY` | 留空表示本地不启用鉴权 |
| `LLM_API_KEY` / `LLM_BASE_URL` | 主模型调用凭据 |
| `EMBEDDING_API_KEY` / `EMBEDDING_BASE_URL` | 向量化模型调用凭据 |

此外，compose 还识别一批**可选**的端口覆盖变量（不写则用默认值，因此 `.env.example` 里没有它们）：

`MYSQL_PORT`、`REDIS_PORT`、`MINIO_API_PORT`、`MINIO_CONSOLE_PORT`、`QDRANT_HTTP_PORT`、`QDRANT_GRPC_PORT`、`MINIO_BUCKET`、`TZ`。

## 常用命令

```bash
make help          # 列出全部命令
make up            # 启动全部基础设施
make down          # 停止并删除容器（保留数据卷）
make clean         # 停止并删除容器 + 数据卷（清空本地数据，需确认）
make ps            # 查看容器状态
make health        # 查看健康检查状态
make logs          # 跟踪全部日志；make logs mysql 只看某个服务
make config        # 校验 compose 语法与变量插值
make mysql-shell   # 进入 MySQL 客户端
make redis-cli     # 进入 Redis 客户端
make urls          # 打印各服务地址
make api-build     # 编译打包 platform-api
make api-run       # 前台启动 platform-api（启动时自动执行 Flyway 迁移）
make api-test      # 运行 platform-api 测试
make db-tables     # 查看 MySQL 里的表与迁移记录
```

## 数据与持久化

容器状态存在 Docker 命名卷中：`agentforge-mysql-data`、`agentforge-redis-data`、`agentforge-minio-data`、`agentforge-qdrant-data`，`make down` 不会删除它们。

仓库内的 `data/` 目录用途不同：它是 Agent 的 RAG 原始语料（`data/blog` 放文章、`data/repo` 放代码仓库），属于业务输入数据，不是数据库存储目录。

`infra/mysql/init.sql` 只在数据卷**首次初始化**时执行一次；改动该文件后需要 `make clean` 再 `make up` 才会生效。

## 数据库迁移（Flyway）

**分工：`init.sql` 管「库」，Flyway 管「表」。**

```
docker compose up
  → MySQL 首次初始化：执行 infra/mysql/init.sql（建库 agentforge + 固定 utf8mb4）
  → platform-api 启动
  → Flyway 扫描 classpath:db/migration
  → 执行 V1__init_schema.sql：建 project / conversation / message / trace_span / document / chunk / eval_* 等业务表
  → 迁移记录写入 flyway_schema_history 表（已执行过的脚本不会重复执行）
```

- 建表脚本位置：`platform-api/src/main/resources/db/migration/`，命名必须是 `V{版本}__{描述}.sql`（**两个下划线**），例如 `V1__init_schema.sql`、`V2__add_task_table.sql`。
- **不要修改已执行过的脚本**：Flyway 会校验文件的 checksum，改了之后启动直接报错。要改结构就新增 `V2__xxx.sql`。
- 本地想把表和库都推倒重来：`make clean`（删数据卷）→ `make up` → `make api-run`。
- 表结构**不要**写进 `init.sql`——那样会绕过 Flyway 的版本管理，两处定义必然打架。

启动 platform-api（先确保 MySQL 已起来：`make up`）：

```bash
make api-build     # 首次会下载依赖，耗时较长
make api-run       # 启动后日志里能看到 Flyway 的 "Migrating schema ... to version 1"
make db-tables     # 另一个终端：确认表已建好
```

应用默认连 `127.0.0.1:${MYSQL_PORT}`（`.env` 里是 3307，因为本机 3306 被占用），账号密码取 `MYSQL_ROOT_PASSWORD`。这些值来自仓库根目录的 `.env`——`application.yml` 通过 `spring.config.import` 导入它，因此无需再维护第二份配置；也可以用同名环境变量覆盖。

## 已知注意事项

1. **3306 端口已被本机 mysqld 占用**（Homebrew 安装的 MySQL 正在监听）。若要同时运行容器版 MySQL，请在 `.env` 中加入 `MYSQL_PORT=3307`，此时容器 MySQL 的地址变为 `127.0.0.1:3307`。
2. **`.env` 里的值不要直接写 `$`**：compose 对 `.env` 取出的值会再做一次插值，`MYSQL_ROOT_PASSWORD=pa$word` 实际会变成 `pa`（`$word` 被当作未定义变量吃掉）。密码含 `$` 时必须写成 `$$`（`pa$$word` 才是 `pa$word`）。这条同时影响 `make urls`，因为它用 shell 加载同一个 `.env`。
3. **镜像 tag**：`mysql:8.4`、`redis:7.4-alpine` 已固定版本；`minio/minio:latest`、`qdrant/qdrant:latest`、`minio/mc:latest` 因官方主要以 `latest` / 日期型 tag 发布而暂用 `latest`。首次 `pull` 成功后建议改为具体 tag 以保证可复现（可用 `docker image inspect --format '{{index .RepoTags 0}}'` 确认实际版本）。
4. **Qdrant 鉴权**：`.env` 中 `QDRANT_API_KEY` 留空时不启用鉴权。若填写了 key，所有客户端请求都必须带 `api-key` 头，否则会返回 401。若留空却遇到 401，请注释掉 `docker-compose.yml` 中 `QDRANT__SERVICE__API_KEY` 一行再重启。
5. **`data/` 目录不会进版本库**：`.gitignore` 忽略了整个 `data/`，因此 `data/blog`、`data/repo` 两个空目录不会被 git 跟踪。若希望保留目录结构，需要补充 `!data/blog/.gitkeep` 之类的例外规则。

## 下一步

- 阶段 3：`agent-runtime` 搭建 FastAPI 骨架（`app/{api,agent,clients,rag,tools}`）与依赖管理（uv）。
- 阶段 4：`web` 初始化 Vite + React + TypeScript，对接 platform-api。
