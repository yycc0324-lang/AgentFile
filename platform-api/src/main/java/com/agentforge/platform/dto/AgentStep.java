package com.agentforge.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Agent 执行步骤。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentStep {

    @JsonProperty("type")
    private String type;

    @JsonProperty("tool")
    private String tool;

    @JsonProperty("args")
    private Map<String, Object> args;

    @JsonProperty("status")
    private String status;

    @JsonProperty("result_count")
    private Integer resultCount;
}
