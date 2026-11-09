package com.agentforge.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.util.ArrayList;
import java.util.List;
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

    @JsonProperty("repo_url")
    private String repoUrl;

    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("history")
    @Builder.Default //没有它，builder 不传 history 时字段是 null
    private List<AgentHistoryItem> history = new ArrayList<>();

}
