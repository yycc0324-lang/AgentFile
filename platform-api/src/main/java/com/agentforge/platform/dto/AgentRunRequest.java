package com.agentforge.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Java -> Python /agent/run 请求体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunRequest {

    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("project_id")
    private Long projectId;

    @JsonProperty("conversation_id")
    private Long conversationId;

    @JsonProperty("message_id")
    private Long messageId;

    @JsonProperty("query")
    private String query;

    @JsonProperty("show_citations")
    private Boolean showCitations;

    @JsonProperty("stream")
    private Boolean stream;
}
