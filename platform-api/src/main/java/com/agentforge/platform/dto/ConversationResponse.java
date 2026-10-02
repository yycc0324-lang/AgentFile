package com.agentforge.platform.dto;

import com.agentforge.platform.entity.Conversation;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话响应。
 */
@Data
public class ConversationResponse {

    private Long id;
    private Long projectId;
    private String title;
    private LocalDateTime createdAt;

    public static ConversationResponse from(Conversation conversation) {
        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setProjectId(conversation.getProjectId());
        response.setTitle(conversation.getTitle());
        response.setCreatedAt(conversation.getCreatedAt());
        return response;
    }
}
