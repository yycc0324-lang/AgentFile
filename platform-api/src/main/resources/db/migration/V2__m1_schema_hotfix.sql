-- ============================================================================
-- V2：M1 必要字段补丁
-- ============================================================================
-- 说明：
--   1. 本迁移在 V1 之后执行，给已存在的表补充 M1 必须字段。
--   2. 不要把这里的内容合回 V1；已执行过的 V1 不能改 checksum。
--   3. 字段作用：
--      project.repo_url / project.blog_path
--        -> 数据接入时，Java/Python 知道去扫描哪个代码仓库和博客目录。
--      task.trigger_message_id
--        -> 精确记录“这个任务是回答哪一条用户消息”，避免只能按时间猜。
--      eval_run.model
--        -> 每次评估记录使用的模型，保证评估结果可复现、可对比。
-- ============================================================================

ALTER TABLE project
    ADD COLUMN repo_url VARCHAR(1000) COMMENT '代码仓库地址（也可以是本地路径）' AFTER description;

ALTER TABLE project
    ADD COLUMN blog_path VARCHAR(1000) COMMENT '博客目录路径（本地目录或仓库内相对路径）' AFTER repo_url;

ALTER TABLE task
    ADD COLUMN trigger_message_id BIGINT COMMENT '触发这个任务的用户消息ID' AFTER conversation_id;

ALTER TABLE task
    ADD INDEX idx_trigger_message_id (trigger_message_id);

ALTER TABLE eval_run
    ADD COLUMN model VARCHAR(128) COMMENT '本次评测使用的模型' AFTER name;


CREATE TABLE IF NOT EXISTS message (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       conversation_id BIGINT NOT NULL,
                                       `role` VARCHAR(32) NOT NULL,
                                       content MEDIUMTEXT,
                                       model VARCHAR(128),
                                       prompt_tokens INT,
                                       completion_tokens INT,
                                       total_tokens INT,
                                       latency_ms INT,
                                       status VARCHAR(32) NOT NULL DEFAULT 'success',
                                       error_message TEXT,
                                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                       INDEX idx_conversation_id (conversation_id),
                                       INDEX idx_created_at (created_at)
);