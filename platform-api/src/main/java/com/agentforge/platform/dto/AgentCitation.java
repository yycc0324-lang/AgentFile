package com.agentforge.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Python 返回的引用信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentCitation {

    @JsonProperty("source_type")
    private String sourceType;

    @JsonProperty("file")
    private String file;

    @JsonProperty("line")
    private Integer line;

    @JsonProperty("snippet")
    private String snippet;
}
