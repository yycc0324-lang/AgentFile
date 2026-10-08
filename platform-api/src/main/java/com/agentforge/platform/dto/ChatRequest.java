package com.agentforge.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 前端 POST /api/chat 请求。
 */
@Data
public class ChatRequest {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    @NotBlank(message = "消息内容不能为空")
    private String message;

    private Boolean showCitations = true;
}
