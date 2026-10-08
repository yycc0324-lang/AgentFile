package com.agentforge.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 消息表。
  JPA用，后期会迭代
 */
// TODO 等项目到M3阶段把JPA迭代为Mybatis-plus
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "message")
public class Message {

    /**
     * 消息主键。
     * 对应 message.id。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属会话 ID。
     * 对应 message.conversation_id。
     */
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    /**
     * 消息角色。
     *
     * user / assistant / system / tool
     *
     * MySQL 里 role 不是保留字，所以这里不需要反引号。
     */
    @Column(name = "role", nullable = false, length = 32)
    private String role;

    /**
     * 消息正文。
     * 数据库字段是 MEDIUMTEXT，可以存长文本。
     */
    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;

    /**
     * 生成这条消息使用的模型。
     * 用户消息通常为 null；助手消息可能有值。
     */
    @Column(name = "model", length = 128)
    private String model;

    /**
     * 输入 token 数。
     */
    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    /**
     * 输出 token 数。
     */
    @Column(name = "completion_tokens")
    private Integer completionTokens;

    /**
     * 总 token 数。
     */
    @Column(name = "total_tokens")
    private Integer totalTokens;

    /**
     * 端到端耗时，单位毫秒。
     */
    @Column(name = "latency_ms")
    private Integer latencyMs;

    /**
     * 消息状态。
     *
     * success / error
     */
    @Column(name = "status", nullable = false, length = 32)
    private String status = "success";

    /**
     * 失败原因。
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 创建时间。
     * 插入时由 Hibernate 自动填值。
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}