# AgentForge 项目开发文档

> 版本：v0.1  
> 状态：总体设计 / M1 准备启动  
> 最后更新：2026-09-27  
> 关联阅读：精读 20 篇（第 1~20 篇）  
> 项目定位：面向技术博客 + 开源仓库的研发知识、Coding Agent 与内容创作平台

---

## 1. 项目概述

### 1.1 一句话定义

AgentForge 是一个 Agent 工作台：接入博客、文档和 Git 仓库，提供代码库问答、Issue 分析、代码修改、Sandbox 执行、测试、PR 生成、技术文章生成和内容评估能力。

### 1.2 为什么做这个项目

- 有真实使用场景：自己的博客和开源仓库就是数据和用户。
- 能覆盖精读 20 篇中的大部分技术点。
- 能同时体现 Java 后端、Python Agent Runtime、前端、基础设施和 AI 工程能力。
- 可以按 M1/M2/M3 迭代，每一阶段都能单独演示，降低烂尾风险。

### 1.3 目标用户

1. 个人技术博主：把代码、文档和思考转成文章、架构图和短视频脚本。
2. 研发团队：做代码库问答、Issue 分析、自动修复和 PR Review。
3. Agent 学习者：完整实践 Agent Runtime、Memory、RAG、Sandbox、Eval、Observability。

### 1.4 非目标

- 不做模型训练和微调。
- 不做通用 SaaS 多租户平台，M1/M2 先单用户/单团队。
- M1 不做复杂多 Agent，不做 K8s，不做插件市场。
- 不为了堆技术而引入 Kafka、Flink、Service Mesh 等非必要组件。

---

## 2. 核心场景

### 2.1 Repo Copilot

用户对仓库或博客提问：

- “这个项目的入口在哪里？”
- “订单创建到支付成功经过哪些模块？”
- “博客里关于 Agent Memory 的文章有哪些？”
- “这个报错可能和哪些代码有关？”

Agent 需要：

- 理解问题意图
- 选择知识检索或代码检索
- 读取相关文档/代码
- 给出带引用的回答
- 记录完整执行 trace

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

### 2.4 AgentOps 平台

统一管理：

- 模型与路由
- Prompt 版本
- 工具与 MCP
- 任务与队列
- Memory
- Trace / Metrics
- 数据集与评估
- 权限与审批
- Sandbox 会话

---

### 2.5 数据接入与爬虫

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

### 2.6 引用显示

用户在提问时可以选择：

```text
showCitations = true  -> 回答后展示引用文献：来源、文件路径、行号、片段
showCitations = false -> 只给答案，不展示引用
```

M1 实现为请求参数；M2 再落库为会话偏好。

## 3. 总体架构

### 3.1 架构图

```text
┌─────────────────────────────────────────────┐
│                 Web 工作台                   │
│ React + TypeScript + Vite + Monaco           │
│ 对话 / 任务 / Trace / Eval / Prompt / 代码预览  │
└──────────────────────┬──────────────────────┘
                       │ REST + SSE
┌──────────────────────▼──────────────────────┐
│           Java Platform API                  │
│ Spring Boot 3 + Spring AI / LangChain4j      │
│ 网关 / 鉴权 / Prompt / 任务 / 评估 / 审计       │
└───────┬──────────────────────────┬───────────┘
        │                          │
┌───────▼────────┐        ┌────────▼────────────┐
│ MySQL 8        │        │ Redis 7              │
│ 业务/任务/评估  │        │ 缓存/队列/锁/限流     │
└───────┬────────┘        └────────┬────────────┘
        │                          │
┌───────▼────────┐        ┌────────▼────────────┐
│ Qdrant          │        │ MinIO                │
│ 向量检索/RAG    │        │ 文件/产物/Trace 归档  │
└───────┬────────┘        └────────┬────────────┘
        │                          │
┌───────▼──────────────────────────▼────────────┐
│            Python Agent Runtime                │
│ FastAPI + LangGraph + Pydantic                 │
│ Router / Planner / Coder / Reviewer / Evaluator│
│ Memory / RAG / Tools / MCP / Sandbox           │
└───────┬──────────────────────────┬────────────┘
        │                          │
┌───────▼────────┐        ┌────────▼────────────┐
│ OpenTelemetry  │        │ MCP / GitHub / Shell │
│ + Langfuse     │        │ / Browser / Sandbox  │
└────────────────┘        └──────────────────────┘
```

### 3.2 技术选型与理由

| 技术 | 用途 | 为什么合理 |
|---|---|---|
| Java 21 + Spring Boot 3 | 平台 API、网关、任务、评估、权限 | 利用你的 Java/Spring 背景，适合企业级平台 |
| Spring AI / LangChain4j | 模型调用、工具抽象、RAG 接口 | Java 侧 AI 能力入口，和 Spring 生态一致 |
| Python 3.12 + FastAPI | Agent Runtime | Python 的 Agent/MCP/Sandbox 生态更成熟 |
| LangGraph | 状态图、长任务、中断恢复 | 比 if-else 更适合复杂 Agent 流程 |
| MCP | 工具标准化接入 | 避免每个工具写一套适配 |
| MySQL 8 | 业务数据、Prompt、任务、数据集 | 你有 MySQL 经验，事务和查询成熟 |
| Redis 7 | 缓存、队列、锁、限流 | 长任务和 Agent 平台的常用基础设施 |
| Qdrant | 向量检索 | RAG 独立演进，避免业务库耦合 |
| MinIO / S3 | 文件、仓库归档、Trace 产物 | 大文件不适合放数据库 |
| Docker | Sandbox 与本地部署 | 代码执行和依赖隔离 |
| K8s（M3） | 生产部署和弹性 | 只有需要多用户/多任务时才引入 |
| OpenTelemetry + Langfuse | Trace / Metrics / LLM 可观测 | Agent 不确定性强，必须可调试 |
| React + TypeScript | 工作台 UI | 对话、Trace、Eval、Prompt 管理需要可视化 |
| GitHub Actions | CI + Eval 回归 | 每次修改自动跑测试和评估 |
| Ragas / 自定义评估 | RAG 与 Agent 指标 | 评估是 Agent 生产化的分水岭 |

### 3.3 模块划分

```text
agentforge/
├── platform-api/          # Java / Spring Boot
│   ├── auth/              # 登录与项目权限
│   ├── gateway/           # API 聚合、SSE、限流
│   ├── prompt/            # Prompt 版本管理
│   ├── task/              # 任务与队列
│   ├── dataset/           # 数据集与评估
│   ├── trace/             # Trace 查询
│   ├── tool/              # 工具与 MCP 注册
│   ├── memory/            # Memory 管理
│   └── audit/             # 审批与审计
├── agent-runtime/         # Python / FastAPI / LangGraph
│   ├── core/              # Agent Loop / State / Checkpoint
│   ├── agents/            # Knowledge / Coding / Content / Reviewer
│   ├── tools/             # 文件、检索、GitHub、Shell
│   ├── mcp/               # MCP 客户端与工具适配
│   ├── memory/            # 短期/任务/长期记忆
│   ├── rag/               # 切块、Embedding、检索、重排
│   ├── sandbox/           # Docker 执行环境
│   ├── eval/              # 评估协调器
│   └── observability/     # OTel / Langfuse 接入
├── web/                   # React + TypeScript
├── infra/                 # docker-compose / k8s / otel
├── docs/                  # 架构、ADR、API、Runbook
└── tests/                 # 集成测试 / E2E
```


### 3.4 Java / Python 职责边界与调用契约

> 一句话分工：Java 是平台层，负责对外、落库、任务和 SSE；Python 是 Agent 层，负责思考、工具、RAG、爬虫和模型调用。

#### Java Platform API 负责

- 前端 REST API：projects / conversations / chat / traces / eval / ingest
- MySQL 全部写操作：所有业务表
- 任务生命周期：创建 task，根据 Python 结果标记成功/失败
- SSE 推送到前端
- 数据接入入口：接收 ingest 请求，调用 Python，回写 document / chunk
- 爬虫任务入口：接收爬虫源配置，调用 Python 执行采集
- M3 再做：权限、Prompt 版本、模型网关、审计、审批

#### Python Agent Runtime 负责

- Router：knowledge_agent / code_agent
- ReAct Loop：思考 -> 工具 -> 观察 -> 再思考
- 工具：search_docs / search_code / read_file / list_files
- RAG：切块、Embedding、Qdrant 写入、向量检索、混合检索、引用生成
- 爬虫：HTML 抓取、正文抽取、Markdown 化、去重
- 模型调用：LLM 和 Embedding 模型，返回 token 用量
- Trace 构建：生成结构化执行轨迹返回给 Java
- M2/M3 再做：LangGraph、Memory、Sandbox、MCP

#### 数据归属

| 存储 | 写方 | 读方 |
|---|---|---|
| MySQL | Java | Java，Python 不直接读写 |
| Qdrant | Python | Python |
| MinIO | Java（文件上传） / Python（爬虫归档） | Java / Python |
| Redis | Java（M2 队列） | Java |

#### 调用方向

```text
Web -> Java -> Python
```

Python 不反向调用 Java。所有结果由 Python 返回给 Java，由 Java 落库和推送前端。

#### Java -> Python 核心契约

```http
POST /agent/run
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

#### 数据接入 / 爬虫契约

```http
POST /ingest/run
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

Python 返回 documents 与 chunks；Java 写 document / chunk，Python 写 Qdrant。
---

## 4. 详细设计

### 4.1 Agent Runtime

#### 4.1.1 执行模型

M1 采用：

- Router 决定任务类型
- ReAct 循环执行
- 工具调用 + Observation 回填
- 最大步数 12
- 超时 120 秒
- 工具失败最多重试 2 次
- 最终答案生成后可选一次 Reflection

M2 升级为：

- LangGraph 状态图
- Planner / Coder / Reviewer / Tester 多角色
- Checkpoint + Resume
- 长任务 Harness
- 文件系统状态
- Completion Contract

#### 4.1.2 状态定义

```python
class AgentState(TypedDict):
    task_id: str
    session_id: str
    messages: list
    plan: list
    current_step: int
    artifacts: list
    tool_results: list
    memory_context: str
    eval_context: str
    status: str
```

#### 4.1.3 错误恢复

- 工具超时：重试一次，再失败返回结构化错误
- 模型输出格式错误：用 JSON Schema 校验并要求重试
- 上下文超限：压缩 + 检索，不直接截断
- 任务中断：从 Checkpoint 恢复
- 高风险动作：进入人工审批

### 4.2 Tool / MCP 设计

工具必须包含：

- 名称
- 描述
- JSON Schema
- 权限等级
- 超时
- 幂等标识
- 错误码
- 审计字段

M1 工具：

- `search_docs`
- `search_code`
- `read_file`
- `list_files`

M2 工具：

- `git_status`
- `git_diff`
- `apply_patch`
- `run_tests`
- `github_list_issues`
- `github_create_pr`
- `browser_open`
- `run_shell`（Sandbox 内）

M3 工具：

- MCP Server 动态注册
- 企业 API OpenAPI 转工具
- 内容发布工具

### 4.3 RAG / Context / Memory

#### RAG

- 文档来源：博客 Markdown、README、docs、代码文件
- 切块：Markdown 按标题；代码按符号/文件
- Embedding：模型可配置
- 检索：向量 + 关键词混合
- 重排：M2 引入 Cross-Encoder 或 LLM 重排
- 引用：返回文件路径、行号、片段

#### Memory

三层：

1. 短期：当前会话消息
2. 任务：计划、状态、检查点、产物
3. 长期：用户偏好、项目规范、术语、历史决策

写入策略：

- 事实和偏好抽取
- 去重
- 冲突消解
- 置信度
- 敏感信息过滤
- 来源和时间戳

### 4.4 Sandbox

M2 实现：

- Docker 容器
- 非 root 用户
- 只读输入目录
- 可写输出目录
- 无特权、无挂载
- CPU/内存/磁盘/时长配额
- 网络白名单
- 命令审计
- 执行结果归档到 MinIO

M3 可选：

- gVisor / Firecracker
- K8s Job 化沙箱
- 按会话回收

### 4.5 Model Gateway

- 多模型路由：DeepSeek / OpenAI / Claude / 本地模型
- 降级与重试
- Token / 成本统计
- Prompt Cache
- 限流与配额
- 审计日志

### 4.6 Evaluation

指标：

- 答案正确性
- 引用准确性
- 忠实度 Faithfulness
- 相关性 Answer Relevance
- 检索 Precision / Recall
- 工具调用正确率
- 任务成功率
- 人工接管率
- 延迟与成本

评估类型：

- 端到端
- 步骤级
- 回归评估
- 线上反馈

数据集：

- 20 条 M1 黄金集
- 100+ 条 M3 数据集
- 合成数据 + 真实数据
- 版本化管理

### 4.7 Observability

Trace 覆盖：

- HTTP 请求
- Agent 步骤
- 模型调用
- 工具调用
- 检索
- Sandbox 执行
- 评估

指标：

- 请求量、成功率、P95 延迟
- Token、成本
- 工具失败率
- 重试率
- 评估分数
- 人工接管率

### 4.8 权限与审计

- 用户身份
- 项目角色
- 工具权限
- 写操作审批
- 危险命令拦截
- 审计日志
- 敏感信息脱敏

---

## 5. 数据模型（核心表）

> 说明：M1 只建 13 张业务表 + 1 张 flyway_schema_history。  
> 其余表（memory / prompt_version / approval / audit_log / project_source / crawl_job / user / project_member）按阶段在 M2/M3 再建。

### M1 已建表

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
flyway_schema_history
```

### 核心字段

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

### M2/M3 再建

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

## 6. API 设计（M1）

### 6.1 项目

```http
POST /api/projects
GET  /api/projects
GET  /api/projects/{id}
```

### 6.2 数据接入 / 爬虫

```http
POST /api/projects/{id}/ingest
GET  /api/projects/{id}/documents
```

ingest 请求体：

```json
{
  "projectId": 1,
  "sources": [
    {"type":"git","uri":"https://github.com/xxx/repo"},
    {"type":"blog","uri":"/path/to/blog"},
    {"type":"url","uri":"https://example.com/article"},
    {"type":"crawl","uri":"https://example.com/docs","config":{"max_depth":2}}
  ]
}
```

说明：

- `git` / `blog` 来源由 `project.repo_url` / `project.blog_path` 提供默认值。
- `url` / `crawl` 来源走 Python 爬虫管线：抓取 -> 正文抽取 -> Markdown -> 切块 -> Embedding -> Qdrant。
- Java 写 `document` / `chunk`，Python 写 Qdrant。

### 6.3 对话

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

`showCitations`：

- `true`：回答后展示引用文献（来源、文件路径、行号、片段）。
- `false`：只给答案，不展示引用。
- M1 作为请求参数；M2 再落库为会话偏好。

返回 SSE 事件：

```text
event: route
data: {"agent":"code_agent"}

event: step
data: {"type":"tool_call","tool":"search_code","args":{...}}

event: step
data: {"type":"observation","result":{...}}

event: answer
data: {"content":"...","citations":[...]}

event: done
data: {"usage":{"prompt_tokens":3200,"completion_tokens":180}}
```

### 6.4 Trace

```http
GET /api/traces/{traceId}
GET /api/tasks/{taskId}/steps
```

### 6.5 评估

```http
POST /api/eval/datasets
POST /api/eval/runs
GET  /api/eval/runs/{id}
```

## 7. 迭代路线

### M1：最小可用 Agent（对应 1~6 篇）

目标：Repo + 文档问答，Router + 工具调用 + Trace + 20 条评估集。

功能：

- 项目创建，填写 `repo_url` / `blog_path`
- 数据接入：Git 仓库 + 博客目录 + 手动上传 + 单个 URL 抓取
- 爬虫基础：抓取 URL、正文抽取、Markdown 化、去重入库
- 向量检索
- Router 路由
- ReAct 循环
- 4 个工具
- SSE 对话
- 回答带引用，支持 `showCitations` 开关
- Trace 展示
- 20 条评估集

技术：

- Spring Boot + Spring AI
- Python FastAPI + 自研 Loop 或 LangGraph 基础
- MySQL + Redis + Qdrant + MinIO
- React + TypeScript
- Docker Compose

验收：

- [ ] 20 条问题至少 16 条正确
- [ ] 回答带引用
- [ ] Router 可区分文档/代码问题
- [ ] 工具失败可重试
- [ ] 每次执行有完整 Trace
- [ ] 一条命令启动

### M2：长任务 Coding Agent（对应 7~13 篇）

目标：Issue → 修改 → 测试 → PR。

功能：

- LangGraph 状态图
- Planner / Coder / Reviewer / Tester
- Memory 三层结构
- Checkpoint / Resume
- Completion Contract
- Docker Sandbox
- GitHub MCP
- 代码库索引
- 步骤级评估
- OTel / Langfuse Trace

验收：

- [ ] 给定 Issue 能生成修改计划
- [ ] 能在 Sandbox 中修改代码并跑测试
- [ ] 失败后能反思重试
- [ ] 能生成 diff / PR
- [ ] 中断后可恢复
- [ ] 能记住项目规范
- [ ] 有步骤级评估报告

### M3：AgentOps 平台 + Content Agent（对应 14~20 篇）

目标：平台化、评估体系、内容生产和部署。

功能：

- Prompt 版本管理
- 模型网关
- 工具/MCP 注册中心
- 数据集与评估 UI
- 评估回归 CI
- 合成测试集生成
- Content Agent：博客、架构图、短视频脚本
- Trace / Metrics Dashboard
- 权限与审批
- Docker/K8s 部署
- 多项目支持

验收：

- [ ] 三个场景共用同一平台：知识、Coding、Content
- [ ] 100+ 评估样本
- [ ] CI 中自动跑 Eval 回归
- [ ] Trace / Metrics 可视化
- [ ] 权限和审批可用
- [ ] Docker Compose / K8s 可部署
- [ ] README + 架构图 + 演示视频

---

## 8. M1 详细执行计划（读完 1~6 篇后）

### 第 1 周：脚手架

- [ ] 创建 monorepo
- [ ] Spring Boot 项目启动
- [ ] Python FastAPI 启动
- [ ] React 项目启动
- [ ] Docker Compose 启动 MySQL/Redis/Qdrant/MinIO
- [ ] 模型 API 调用跑通
- [ ] 定义 API 契约

### 第 2 周：Agent 核心

- [ ] 实现 Router
- [ ] 实现 ReAct Loop
- [ ] 实现 `search_docs`
- [ ] 实现 `search_code`
- [ ] 实现 `read_file`
- [ ] 实现 `list_files`
- [ ] 工具参数校验与错误处理
- [ ] 10 条问题跑通

### 第 3 周：RAG + Trace

- [ ] Markdown 切块
- [ ] 代码切块
- [ ] Embedding 入库
- [ ] 向量检索 + 引用
- [ ] Trace 数据结构
- [ ] Trace 查询 API
- [ ] 前端 Trace 展示
- [ ] SSE 流式输出

### 第 4 周：评估 + 收尾

- [ ] 20 条黄金评估集
- [ ] 人工评分表
- [ ] 失败案例分析
- [ ] README
- [ ] 架构图
- [ ] 演示视频
- [ ] 代码提交与 Tag

---

## 9. 测试策略

- 单元测试：工具、路由、Schema、状态机
- 集成测试：API + Agent Runtime + DB
- E2E：从提问到答案的完整链路
- 评估回归：每次修改跑黄金集
- 安全测试：Prompt 注入、工具越权、Sandbox 逃逸
- 性能测试：并发对话、长任务、Token 成本

---

## 10. 部署与运维

### 本地

```bash
docker compose up -d
```

### 生产

- 前端：Nginx / CDN
- 平台 API：Spring Boot 容器
- Agent Runtime：FastAPI 容器
- MySQL / Redis / Qdrant / MinIO：独立服务
- 可观测：OTel Collector + Langfuse + Prometheus + Grafana
- 发布：GitHub Actions 构建镜像 + 部署

### 配置管理

- `application.yml`
- `.env`
- Docker secrets / K8s Secret
- 禁止把 API Key 写进代码和前端

---

## 11. 面试表达模板

**项目一句话：**
> 我设计并实现了一个 AgentForge 平台，把博客、文档和 Git 仓库接入 Agent，支持知识问答、Issue 到 PR 的 Coding Agent、以及技术内容生成；平台包含 Runtime、Memory、RAG、Sandbox、评估和可观测性。

**可展开的技术点：**

- Agent Loop 与状态图
- Router 与工具调用
- RAG 与引用
- Memory 分层
- 长任务 Harness 与 Checkpoint
- Sandbox 安全
- OpenTelemetry Trace
- 评估集与 LLM-as-judge
- Java 平台 + Python Runtime 的边界设计

**面试官可能追问：**

- 为什么 Java + Python 双栈？
- 为什么用 Qdrant 而不是 pgvector？
- 多 Agent 边界怎么划？
- Memory 怎么防污染？
- Sandbox 怎么防止逃逸？
- 如何评估 Agent 改进有效？
- 成本和延迟怎么控制？

---

## 12. 风险与对策

| 风险 | 对策 |
|---|---|
| 项目太大烂尾 | M1 只做最小闭环，按周验收 |
| 双栈复杂 | API 契约先定，错误边界清晰 |
| 评估数据太少 | 先 20 条真实问题，逐步扩 |
| Agent 不稳定 | 最大步数、超时、重试、人工接管 |
| Sandbox 安全 | 非 root、无特权、网络白名单、审计 |
| 成本失控 | 模型路由、Token 统计、Prompt Cache |
| 技术堆砌 | 每个技术必须对应真实问题，否则推迟 |

---

## 13. 立即开始清单

- [ ] 创建 `agentforge` 仓库
- [ ] 建 `platform-api`、`agent-runtime`、`web`
- [ ] 写第一版 `docker-compose.yml`
- [ ] 定 `POST /api/chat` 契约
- [ ] 实现 `search_docs`
- [ ] 10 条测试问题
- [ ] 生成第一张架构图
- [ ] 用 MemOS 记录每个设计决策

---

> 记住：M1 的目标不是“功能多”，而是“闭环跑通、可演示、可评估”。  
> M1 跑通后，后面每一篇精读都会变成给这个项目加一块真正需要的能力。
