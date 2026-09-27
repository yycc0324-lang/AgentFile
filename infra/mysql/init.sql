-- AgentForge MySQL 初始化脚本
--
-- 由 docker-compose 挂载到容器内 /docker-entrypoint-initdb.d/，
-- 仅在 mysql-data 卷【首次初始化】时执行一次。
-- 因此：修改本文件后必须 `make clean`（删除数据卷）再 `make up` 才会重新生效。
--
-- 约定：本文件只做「库级初始化」，业务表结构交给 platform-api 的迁移工具
-- （Flyway / Liquibase）管理，不要在这里创建业务表，避免与迁移脚本冲突。

SET NAMES utf8mb4;

-- MYSQL_DATABASE 已创建同名库，此处仅作兜底并显式固定字符集
CREATE DATABASE IF NOT EXISTS `agentforge`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `agentforge`;
