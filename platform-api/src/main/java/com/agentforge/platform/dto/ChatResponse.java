package com.agentforge.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 前端 POST /api/chat 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private Long userMessageId;
    private Long assistantMessageId;
    private String content;
    private List<AgentCitation> citations;
    private String traceId;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer latencyMs;
}
