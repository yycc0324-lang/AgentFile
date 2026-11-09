package com.agentforge.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一轮历史对话。只带 role 和 content，token、耗时这些不需要再回传。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentHistoryItem {

    @JsonProperty("role")
    private String role;

    @JsonProperty("content")
    private String content;
}