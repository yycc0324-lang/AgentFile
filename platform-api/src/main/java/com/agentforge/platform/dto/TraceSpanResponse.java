package com.agentforge.platform.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TraceSpanResponse {

    private Long id;
    private String name;
    private String type;
    private String status;
    private Integer durationMs;
    private String input;
    private String output;
    private String error;
    private List<TraceSpanResponse> children = new ArrayList<>();
}