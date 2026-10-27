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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Agent 任务表,定义好类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(name = "trigger_message_id")
    private Long triggerMessageId;

    @Column(name = "parent_task_id")
    private Long parentTaskId;

    @Column(name = "task_type", length = 32)
    private String taskType;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "input", columnDefinition = "MEDIUMTEXT")
    private String input;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "result", columnDefinition = "MEDIUMTEXT")
    private String result;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "max_steps")
    private Integer maxSteps;

    @Column(name = "current_step")
    private Integer currentStep;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}