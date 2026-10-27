package com.agentforge.platform.dto;

import com.agentforge.platform.entity.Message;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息响应 DTO。
 *
 * 用于 GET /api/conversations/{conversationId}/messages
 */
@Data
public class MessageResponse {

    private Long id;

    private Long conversationId;

    /**
     * user / assistant / system / tool
     */
    private String role;

    private String content;

    private String model;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Integer latencyMs;

    /**
     * success / error
     */
    private String status;

    private String errorMessage;

    private LocalDateTime createdAt;

    /**
     * Entity -> DTO。
     */
    public static MessageResponse from(Message message) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setConversationId(message.getConversationId());
        response.setRole(message.getRole());
        response.setContent(message.getContent());
        response.setModel(message.getModel());
        response.setPromptTokens(message.getPromptTokens());
        response.setCompletionTokens(message.getCompletionTokens());
        response.setTotalTokens(message.getTotalTokens());
        response.setLatencyMs(message.getLatencyMs());
        response.setStatus(message.getStatus());
        response.setErrorMessage(message.getErrorMessage());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }
}