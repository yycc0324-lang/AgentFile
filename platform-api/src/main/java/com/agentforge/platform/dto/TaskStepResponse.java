package com.agentforge.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskStepResponse {

    private Long id;
    private Long taskId;
    private Integer stepIndex;
    private String stepType;
    private String name;
    private String status;
    private String input;
    private String output;
    private Integer durationMs;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}