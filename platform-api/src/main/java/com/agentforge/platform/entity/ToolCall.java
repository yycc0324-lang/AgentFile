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
 * 工具调用记录。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tool_call")
public class ToolCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "step_id")
    private Long stepId;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "tool_name", length = 128)
    private String toolName;

    @Column(name = "arguments", columnDefinition = "JSON")
    private String arguments;

    @Column(name = "result", columnDefinition = "MEDIUMTEXT")
    private String result;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "called_at")
    private LocalDateTime calledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}