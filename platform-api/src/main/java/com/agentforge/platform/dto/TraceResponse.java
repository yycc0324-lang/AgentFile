package com.agentforge.platform.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TraceResponse {

    private String traceId;
    private String status;
    private Integer durationMs;
    private Integer spanCount;
    private Integer tokens;
    private List<TraceSpanResponse> spans = new ArrayList<>();
}