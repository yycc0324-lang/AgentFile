# AgentForge 项目开发文档

> 版本：v0.2（基于 v0.1 修订）  
> 更新：2026-09-29  
> 状态：M1 启动版，已对齐当前代码与数据库  
> 配套文档：`21-AgentForge项目开发文档.md`（原始设计）、`23-AgentForge-M1-M3-任务拆解.md`  
> 项目定位：面向技术博客、开源仓库和爬虫内容源的 Agent 工作台

---

## 1. 项目概述

### 1.1 一句话定义

AgentForge 是一个 Agent 工作台：接入博客、Git 仓库、文档和爬虫内容，提供知识库问答、代码库问答、Coding Agent、内容生成和评估能力。

### 1.2 目标用户

1. 个人技术博主：把博客、代码和爬取内容转成知识库，并生成文章、架构图和短视频脚本。
2. 研发团队：做代码库问答、Issue 分析、自动修复和 PR Review。
3. Agent 学习者：完整实践 Agent Runtime、Memory、RAG、Sandbox、Eval、Observability。

### 1.3 非目标

- 不做模型训练和微调。
- 不做通用 SaaS 多租户平台，M1/M2 先单用户/单团队。
- M1 不做复杂多 Agent，不做 K8s，不做插件市场。
- 不为了堆技术而引入 Kafka、Flink、Service Mesh 等非必要组件。

---

## 2. 核心场景

### 2.1 Repo Copilot

用户对仓库、博客或爬取内容提问：

- “这个项目的入口在哪里？”
- “订单创建到支付成功经过哪些模块？”
- “我博客里关于 Agent Memory 的文章有哪些？”
- “爬取的外部分析文章里，哪些提到了 RAG 评估？”

Agent 需要：

- 理解问题意图
- 选择知识检索或代码检索
- 读取相关文档/代码
- 给出带引用的回答
- 按用户设置决定是否展示引用文献
- 记录完整执行 Trace

### 2.2 Coding Agent

输入一个 GitHub Issue 或自然语言需求：

- 分析 Issue
- 定位代码
- 生成修改计划
- 在 Sandbox 中修改代码
- 运行测试
- 失败后反思、重试
- 生成 diff / PR
- Reviewer Agent 审核

### 2.3 Content Agent

输入一次代码变更、技术方案或学习笔记：

- 生成博客草稿
- 生成文章大纲
- 生成架构图/流程图描述
- 生成公众号、掘金、知乎风格文案
- 生成 60 秒短视频脚本
- 事实性、引用和风格评估

### 2.4 数据接入与爬虫

系统支持四类数据源：

| 来源 | 说明 | 阶段 |
|---|---|---|
| Git 仓库 | `project.repo_url`，克隆或本地路径扫描 | M1 |
| 博客目录 | `project.blog_path`，扫描 Markdown | M1 |
| 手动上传 | Markdown / PDF / 代码文件 | M1 |
| 爬虫内容 | 指定 URL、站内深度抓取、HTML 转 Markdown、去重入库 | M1.5 / M2 |

爬虫内容进入同一条 RAG 管线：

```text
爬虫采集 -> 正文抽取 -> Markdown 化 -> document -> chunk -> Embedding -> Qdrant
```

### 2.5 引用显示

用户在提问时可以选择：

```text
showCitations = true  -> 回答后展示引用文献：来源、文件路径、行号、片段
showCitations = false -> 只给答案，不展示引用
```

M1 实现为请求参数；M2 再落库为会话偏好。

---

## 3. 总体架构

```text
┌─────────────────────────────────────────────┐
│                 Web 工作台                   │
│ React + TypeScript + Vite + Monaco           │
│ 对话 / 任务 / Trace / Eval / 引用展示         │
└──────────────────────┬──────────────────────┘
                       │ REST + SSE
┌──────────────────────▼──────────────────────┐
│           Java Platform API                  │
│ Spring Boot 3                                │
│ 项目/会话/消息/任务/评估/审计/SSE/落库         │
└───────┬──────────────────────────┬───────────┘
        │                          │
        │ MySQL                    │ Redis
        │ 业务/任务/评估            │ 缓存/队列(M2)
        │                          │
        ▼                          ▼
┌───────────────┐        ┌────────────────────┐
│ Qdrant         │        │ MinIO               │
│ 向量检索/RAG    │        │ 文件/爬虫归档/产物   │
└───────▲───────┘        └─────────┬──────────┘
        │                          │
        │                          │
┌───────┴──────────────────────────▼──────────┐
│            Python Agent Runtime              │
│ FastAPI + ReAct + Pydantic                   │
│ Router / Tools / RAG / Crawler / Evaluator   │
│ Memory(M2) / Sandbox(M2) / MCP(M2)           │
└──────────────────────────────────────────────┘
```

---

## 4. Java / Python 职责边界（核心契约）

### 4.1 一句话分工

> **Java 是平台层，负责对外、落库、任务和 SSE。**  
> **Python 是 Agent 层，负责思考、工具、RAG、爬虫和模型调用。**

### 4.2 Java Platform API 负责

- 前端 REST API：projects / conversations / chat / traces / eval / ingest
- MySQL 全部写操作：所有业务表
- 任务生命周期：创建 `task`，根据 Python 结果标记成功/失败
- SSE 推送到前端
- 数据接入入口：接收 ingest 请求，调用 Python，回写 `document` / `chunk`
- 爬虫任务入口：接收爬虫源配置，调用 Python 执行采集
- M3 再做：权限、Prompt 版本、模型网关、审计、审批

### 4.3 Python Agent Runtime 负责

- Router：knowledge_agent / code_agent
- ReAct Loop：思考 -> 工具 -> 观察 -> 再思考
- 工具：search_docs / search_code / read_file / list_files
- RAG：切块、Embedding、Qdrant 写入、向量检索、混合检索、引用生成
- 爬虫：HTML 抓取、正文抽取、Markdown 化、去重
- 模型调用：LLM 和 Embedding 模型，返回 token 用量
- Trace 构建：生成结构化执行轨迹返回给 Java
- M2/M3 再做：LangGraph、Memory、Sandbox、MCP

### 4.4 数据归属

| 存储 | 写方 | 读方 |
|---|---|---|
| MySQL | Java | Java，Python 不直接读写 |
| Qdrant | Python | Python |
| MinIO | Java（文件上传） / Python（爬虫归档） | Java / Python |
| Redis | Java（M2 队列） | Java |

### 4.5 调用方向

```text
Web -> Java -> Python
```

Python 不反向调用 Java。所有结果由 Python 返回给 Java，由 Java 落库和推送前端。

---

## 5. API 契约

### 5.1 Web -> Java

#### 项目

```http
POST /api/projects
GET  /api/projects
GET  /api/projects/{id}
```

#### 数据接入

```http
POST /api/projects/{id}/ingest
GET  /api/projects/{id}/documents
```

#### 对话（SSE）

```http
POST /api/chat
Content-Type: application/json

{
  "projectId": 1,
  "conversationId": 101,
  "message": "订单服务入口在哪里？",
  "showCitations": true
}
```

前端收到 SSE：

```text
event: route
event: step
event: tool_call
event: answer
event: done
```

`answer` 事件示例：

```json
{
  "event": "answer",
  "data": {
    "content": "入口在 OrderController.java:25...",
    "citations": [
      {
        "source_type": "code",
        "file": "src/main/java/com/example/order/OrderController.java",
        "line": 25,
        "snippet": "public class OrderController..."
      }
    ]
  }
}
```

#### Trace 与评估

```http
GET /api/traces/{traceId}
GET /api/tasks/{taskId}/steps

POST /api/eval/datasets
POST /api/eval/runs
GET  /api/eval/runs/{id}
```

### 5.2 Java -> Python

#### Agent 执行

```http
POST /agent/run
Content-Type: application/json

{
  "task_id": "5001",
  "project_id": 1,
  "conversation_id": 101,
  "message_id": 1001,
  "query": "订单服务入口在哪里？",
  "show_citations": true,
  "stream": true
}
```

Python 返回 NDJSON 事件流：

```json
{"event":"route","data":{"agent":"code_agent"}}
{"event":"step_start","data":{"step_index":1,"name":"search_code"}}
{"event":"tool_call","data":{"step_index":1,"tool_name":"search_code","arguments":{...}}}
{"event":"tool_result","data":{"step_index":1,"tool_name":"search_code","result":{...},"duration_ms":120}}
{"event":"step_end","data":{"step_index":1,"name":"search_code","status":"success"}}
{"event":"answer","data":{"content":"...","citations":[...]}}
{"event":"done","data":{"usage":{"prompt_tokens":3200,"completion_tokens":180}}}
```

Java 收到后落库：

```text
route / step 事件 -> task_step
tool_call 事件   -> tool_call
step 事件        -> trace_span
answer 事件      -> assistant message
usage 事件       -> message token 字段
```

#### 数据接入 / 爬虫

```http
POST /ingest/run
Content-Type: application/json

{
  "project_id": 1,
  "sources": [
    {"type":"git","uri":"https://github.com/xxx/repo"},
    {"type":"blog","uri":"/path/to/blog"},
    {"type":"url","uri":"https://example.com/article"},
    {"type":"crawl","uri":"https://example.com/docs","config":{"max_depth":2}}
  ]
}
```

Python 返回：

```json
{
  "documents": [
    {
      "title": "文章标题",
      "source_type": "url",
      "source_uri": "https://example.com/article",
      "content_type": "text/markdown",
      "content_hash": "abc123",
      "chunks": [
        {"chunk_index":0,"content":"...","start_line":1,"end_line":20,"metadata":{}}
      ]
    }
  ]
}
```

Java 写 `document` / `chunk`；Python 写 Qdrant。

---

## 6. 数据模型

### 6.1 M1 已建表（14 张 = 13 业务 + 1 Flyway）

```text
project
conversation
message
task
task_step
tool_call
trace_span
document
chunk
eval_dataset
eval_sample
eval_run
eval_score
flyway_schema_history  <- Flyway 自动维护
```

### 6.2 核心字段

```text
project
  id, name, description, repo_url, blog_path, created_at, updated_at

conversation
  id, project_id, title, created_at

message
  id, conversation_id, role, content, model,
  prompt_tokens, completion_tokens, total_tokens,
  latency_ms, status, error_message, created_at

task
  id, project_id, conversation_id, trigger_message_id,
  parent_task_id, task_type, title, input, status, result,
  error_message, max_steps, current_step,
  created_at, started_at, finished_at, updated_at

task_step
  id, task_id, step_index, step_type, name, status,
  input, output, error_message,
  started_at, ended_at, duration_ms, created_at

tool_call
  id, task_id, step_id, trace_id, tool_name, arguments,
  result, status, error_message, duration_ms, called_at, created_at

trace_span
  id, trace_id, conversation_id, message_id, parent_span_id,
  task_id, step_id, tool_call_id, name, span_type, status,
  input, output, attributes,
  started_at, ended_at, duration_ms, error_message, created_at

document
  id, project_id, title, source_type, source_uri, content_type,
  size_bytes, content_hash, status, chunk_count, error_message,
  created_at, updated_at

chunk
  id, document_id, project_id, chunk_index, start_line, end_line,
  content, token_count, vector_id, embedding_model, metadata, created_at

eval_dataset
  id, project_id, name, description, sample_count, created_at, updated_at

eval_sample
  id, dataset_id, question, expected_answer, expected_doc_ids, tags, created_at

eval_run
  id, dataset_id, project_id, name, model, status, metrics,
  sample_total, sample_passed, started_at, finished_at, created_at

eval_score
  id, run_id, sample_id, answer, retrieved_doc_ids,
  score, passed, metrics, latency_ms, created_at
```

### 6.3 M2/M3 再建

```text
project_source    # 一个项目的多个来源：git / blog / url / crawl
crawl_job         # 爬虫任务、状态、进度、去重、重试
user              # 用户
project_member    # 用户与项目关系
memory            # 长期/任务记忆
prompt_version    # Prompt 版本
approval          # 审批
audit_log         # 审计
eval_metric       # 多指标拆表（如果 M1 的 metrics JSON 不够用）
```

---

## 7. 迭代路线

### M1：最小可用 Agent（Repo + 博客 + 单 URL 接入）

目标：

- 项目创建，填写 `repo_url` / `blog_path`
- 数据接入：Git 仓库 + 博客目录 + 手动上传 + 单个 URL 抓取
- Router + ReAct + 4 个工具
- SSE 对话
- 回答带引用，支持 `showCitations` 开关
- 完整 Trace
- 20 条评估集，至少 16 条正确

### M1.5 / M2：Coding Agent + 爬虫增强

- 爬虫升级：`project_source` + `crawl_job`
- 站内深度抓取、调度、增量更新、去重、失败重试
- Coding Agent：LangGraph、Planner/Coder/Reviewer/Tester
- Sandbox
- GitHub Issue -> PR
- Memory
- Checkpoint / Resume
- MCP

### M3：AgentOps 平台 + Content Agent

- Prompt 版本管理
- 模型网关
- 工具 / MCP 注册中心
- 数据集与评估 UI
- Content Agent
- Trace / Metrics Dashboard
- 权限与审批
- K8s 部署

---

## 8. M1 执行顺序

```text
1. 确认数据库迁移到 V2
2. platform-api：Result<T>、GlobalExceptionHandler
3. platform-api：Project CRUD
4. platform-api：POST /api/chat 骨架 + SSE
5. agent-runtime：FastAPI + /health + /agent/run
6. agent-runtime：Router + ReAct Loop
7. agent-runtime：search_docs / search_code / read_file / list_files
8. agent-runtime：RAG 管线（切块 + Embedding + Qdrant）
9. platform-api：数据接入 /ingest 调用 Python
10. platform-api：Trace 落库与查询
11. web：会话、聊天、引用展示、Trace 时间线
12. 20 条评估集与人工评分
```

---

## 9. 验收标准

- [ ] 一条命令启动全部服务
- [ ] 能从 Git 仓库、博客目录、单个 URL 接入数据
- [ ] 能爬取指定 URL 并转成 Markdown 入库
- [ ] Agent 能区分文档/代码问题
- [ ] 至少 4 个工具可用
- [ ] 每次执行有完整 Trace
- [ ] 答案带引用，且用户可开关引用展示
- [ ] 20 条评估集至少 16 条正确
- [ ] README 有架构图和启动说明
