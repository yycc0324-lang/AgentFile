package com.agentforge.platform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建会话请求。
 */
@Data
public class ConversationCreateRequest {

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @Size(max = 500, message = "会话标题不能超过500个字符")
    private String title;
}
