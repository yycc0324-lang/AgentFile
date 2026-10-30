package com.agentforge.platform.controller;

import com.agentforge.platform.common.Result;
import com.agentforge.platform.dto.TraceResponse;
import com.agentforge.platform.service.AgentTraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/traces")
@RequiredArgsConstructor
public class TraceController {

    private final AgentTraceService agentTraceService;

    @GetMapping("/{traceId}")
    public Result<TraceResponse> getTrace(@PathVariable String traceId) {
        return Result.success(agentTraceService.getTrace(traceId));
    }
}