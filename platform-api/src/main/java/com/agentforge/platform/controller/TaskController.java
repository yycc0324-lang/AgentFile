package com.agentforge.platform.controller;

import com.agentforge.platform.common.Result;
import com.agentforge.platform.dto.TaskStepResponse;
import com.agentforge.platform.service.AgentTraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final AgentTraceService agentTraceService;

    @GetMapping("/{taskId}/steps")
    public Result<List<TaskStepResponse>> listSteps(@PathVariable Long taskId) {
        return Result.success(agentTraceService.listSteps(taskId));
    }
}