package com.agentforge.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Python /agent/run 响应体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunResponse {

    @JsonProperty("content")
    private String content;

    @JsonProperty("citations")
    private List<AgentCitation> citations = new ArrayList<>();

    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("usage")
    private Map<String, Integer> usage = new HashMap<>();
}
