-- ============================================================================
-- AgentForge V1：业务表初始化
-- ============================================================================
-- 执行者：platform-api 启动时的 Flyway（不是 docker-entrypoint-initdb.d）
-- 生效条件：应用每次启动都会扫描本目录，只执行「尚未记录在 flyway_schema_history 表里」的脚本。
--
-- 与 infra/mysql/init.sql 的分工：
--   init.sql     → 只管「库」：CREATE DATABASE + 字符集/排序规则（容器首次初始化时执行一次）
--   本文件        → 只管「表」：所有业务表结构
--   因此本文件里不出现 CREATE DATABASE / USE，库名由 JDBC URL 指定。
--
-- 命名规则（Flyway 硬性要求，注意是两个下划线）：
--   V{版本}__{描述}.sql     例：V1__init_schema.sql、V2__add_task_table.sql
--   已执行过的脚本不要改内容（Flyway 会校验 checksum 并在启动时报错）；
--   要改结构请新增 V2、V3……
--
-- 字符集：表不写 ENGINE/CHARSET，直接继承库级设置（init.sql 里固定为
--   utf8mb4 / utf8mb4_unicode_ci），保证「库管字符集、迁移管结构」这条分工不被绕过。
--
-- 外键：只建索引、不建 FOREIGN KEY 约束。理由是本项目的表会随迭代频繁演进，
--   外键会让迁移顺序、批量导入、清理数据都变得别扭；一致性由应用层保证。
-- ============================================================================


-- ---------------------------------------------------------------- 项目
-- 一切数据的顶层容器：会话、文档、评测集都挂在 project 下。
CREATE TABLE IF NOT EXISTS project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT '项目名',
    description TEXT COMMENT '项目描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '项目';


-- ---------------------------------------------------------------- 会话
-- 一个项目下的多轮对话，消息与调用链都归属到某个会话。
CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '所属项目',
    title VARCHAR(500) COMMENT '会话标题（可由首条用户消息生成）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) COMMENT '会话';


-- ---------------------------------------------------------------- 消息
-- 用户与助手的一问一答；一次助手回复通常还会对应多条 trace_span（见下）。
CREATE TABLE IF NOT EXISTS message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL COMMENT '所属会话',
    `role` VARCHAR(32) NOT NULL COMMENT '角色：user / assistant / system / tool',
    content MEDIUMTEXT COMMENT '消息正文（长文本，用 MEDIUMTEXT）',
    model VARCHAR(128) COMMENT '本次生成使用的模型名',
    prompt_tokens INT COMMENT '输入 token 数',
    completion_tokens INT COMMENT '输出 token 数',
    total_tokens INT COMMENT '总 token 数',
    latency_ms INT COMMENT '端到端耗时（毫秒）',
    status VARCHAR(32) NOT NULL DEFAULT 'success' COMMENT 'success / error',
    error_message TEXT COMMENT '失败原因',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_created_at (created_at)
) COMMENT '会话消息';


-- ---------------------------------------------------------------- 调用链
-- 可观测性核心表：一次请求内的每一步（LLM 调用、工具调用、检索）都是一行 span。
-- 同一链路共享 trace_id，通过 parent_span_id 组成树，用于回放 Agent 的执行过程。
CREATE TABLE IF NOT EXISTS trace_span (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trace_id VARCHAR(64) NOT NULL COMMENT '链路 ID，同一次请求的所有 span 相同',
    conversation_id BIGINT COMMENT '所属会话',
    message_id BIGINT COMMENT '对应的消息（助手回复）',
    parent_span_id BIGINT COMMENT '父 span，为空表示链路根节点',
    name VARCHAR(128) NOT NULL COMMENT 'span 名称，如 llm.chat / tool.search / rag.retrieve',
    span_type VARCHAR(32) NOT NULL DEFAULT 'internal' COMMENT 'llm / tool / retrieval / internal',
    status VARCHAR(32) NOT NULL DEFAULT 'success' COMMENT 'success / error',
    input TEXT COMMENT '入参（提示词、工具参数等）',
    output MEDIUMTEXT COMMENT '出参（模型回复、工具结果等）',
    attributes JSON COMMENT '结构化附加信息：模型参数、命中片段、耗时明细等',
    started_at DATETIME(3) NOT NULL COMMENT '开始时间（毫秒精度）',
    ended_at DATETIME(3) COMMENT '结束时间（毫秒精度）',
    duration_ms INT COMMENT '耗时（毫秒）',
    error_message TEXT COMMENT '失败堆栈或原因',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_trace_id (trace_id),
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_message_id (message_id),
    INDEX idx_started_at (started_at)
) COMMENT 'Agent 执行链路 span';


-- ---------------------------------------------------------------- 知识库文档
-- RAG 的原始语料登记表：一行 = 一份被纳入知识库的文件/网页/仓库文件。
CREATE TABLE IF NOT EXISTS document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '所属项目',
    title VARCHAR(500) COMMENT '文档标题',
    source_type VARCHAR(32) NOT NULL DEFAULT 'file' COMMENT '来源类型：file / url / repo / text',
    source_uri VARCHAR(1000) COMMENT '来源地址或相对路径',
    content_type VARCHAR(128) COMMENT 'MIME 类型，如 text/markdown、application/pdf',
    size_bytes BIGINT COMMENT '原始字节数',
    content_hash CHAR(64) COMMENT '内容 SHA-256，用于去重与增量重新索引',
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending / parsing / chunked / indexed / failed',
    chunk_count INT NOT NULL DEFAULT 0 COMMENT '已切分的 chunk 数量',
    error_message TEXT COMMENT '处理失败原因',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_content_hash (project_id, content_hash),
    INDEX idx_project_id (project_id),
    INDEX idx_status (status)
) COMMENT '知识库文档';


-- ---------------------------------------------------------------- 文档切片
-- 向量化与检索的最小单位；vector_id 指向 Qdrant 里的 point，两库通过它对齐。
CREATE TABLE IF NOT EXISTS chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL COMMENT '所属文档',
    project_id BIGINT NOT NULL COMMENT '冗余的项目 ID，便于按项目直接过滤检索',
    chunk_index INT NOT NULL COMMENT '文档内序号，从 0 开始',
    content MEDIUMTEXT NOT NULL COMMENT '切片正文',
    token_count INT COMMENT '切片 token 数',
    vector_id VARCHAR(64) COMMENT 'Qdrant point id',
    embedding_model VARCHAR(128) COMMENT '生成该向量的模型名',
    metadata JSON COMMENT '页码、标题层级、代码语言等附加元数据',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_document_chunk_index (document_id, chunk_index),
    INDEX idx_project_id (project_id),
    INDEX idx_vector_id (vector_id)
) COMMENT '文档切片';


-- ---------------------------------------------------------------- 评测：数据集
CREATE TABLE IF NOT EXISTS eval_dataset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '所属项目',
    name VARCHAR(255) NOT NULL COMMENT '数据集名称',
    description TEXT COMMENT '用途说明',
    case_count INT NOT NULL DEFAULT 0 COMMENT '题目数量（冗余计数，便于列表展示）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) COMMENT '评测数据集';


-- ---------------------------------------------------------------- 评测：题目
CREATE TABLE IF NOT EXISTS eval_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_id BIGINT NOT NULL COMMENT '所属数据集',
    question TEXT NOT NULL COMMENT '待提问的问题',
    expected_answer TEXT COMMENT '参考答案（用于语义相似度/LLM 打分）',
    expected_doc_ids VARCHAR(500) COMMENT '期望命中的 document id，逗号分隔（用于召回率打分）',
    tags VARCHAR(255) COMMENT '标签，逗号分隔',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_dataset_id (dataset_id)
) COMMENT '评测题目';


-- ---------------------------------------------------------------- 评测：批次
-- 一次评测执行 = 一行；metrics 里放该批次的汇总指标。
CREATE TABLE IF NOT EXISTS eval_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_id BIGINT NOT NULL COMMENT '使用的数据集',
    project_id BIGINT COMMENT '所属项目（冗余，便于列表聚合）',
    name VARCHAR(255) COMMENT '批次名称，如 v1-baseline',
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending / running / success / failed',
    metrics JSON COMMENT '汇总指标：hit_rate / faithfulness / avg_latency_ms 等',
    case_total INT NOT NULL DEFAULT 0 COMMENT '题目总数',
    case_passed INT NOT NULL DEFAULT 0 COMMENT '通过数',
    started_at DATETIME(3) COMMENT '开始时间',
    finished_at DATETIME(3) COMMENT '结束时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_dataset_id (dataset_id),
    INDEX idx_status (status)
) COMMENT '评测批次';


-- ---------------------------------------------------------------- 评测：单题结果
CREATE TABLE IF NOT EXISTS eval_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL COMMENT '所属评测批次',
    case_id BIGINT NOT NULL COMMENT '对应题目',
    answer TEXT COMMENT '系统实际回答',
    retrieved_doc_ids VARCHAR(500) COMMENT '实际召回的 document id，逗号分隔',
    score DECIMAL(6,4) COMMENT '得分，0.0000 ~ 1.0000',
    passed TINYINT NOT NULL DEFAULT 0 COMMENT '是否通过：0=否 1=是',
    metrics JSON COMMENT '该题的细分指标：召回率、相似度、耗时分布等',
    latency_ms INT COMMENT '该题耗时（毫秒）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_run_id (run_id),
    INDEX idx_case_id (case_id)
) COMMENT '评测单题结果';
