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
 * Trace 节点。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "trace_span")
public class TraceSpan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "parent_span_id")
    private Long parentSpanId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "step_id")
    private Long stepId;

    @Column(name = "tool_call_id")
    private Long toolCallId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "span_type", nullable = false, length = 32)
    private String spanType;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "input", columnDefinition = "TEXT")
    private String input;

    @Column(name = "output", columnDefinition = "MEDIUMTEXT")
    private String output;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}