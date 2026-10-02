package com.agentforge.platform.controller;

import com.agentforge.platform.common.Result;
import com.agentforge.platform.dto.ConversationCreateRequest;
import com.agentforge.platform.dto.ConversationResponse;
import com.agentforge.platform.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话接口。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * 创建会话。
     *
     * POST /api/conversations
     */
    @PostMapping("/conversations")
    public Result<ConversationResponse> create(@Valid @RequestBody ConversationCreateRequest request) {
        return Result.success(conversationService.create(request));
    }

    /**
     * 查询某个项目下的会话列表。
     *
     * GET /api/projects/{projectId}/conversations
     */
    @GetMapping("/projects/{projectId}/conversations")
    public Result<List<ConversationResponse>> listByProject(@PathVariable Long projectId) {
        return Result.success(conversationService.listByProject(projectId));
    }
}
