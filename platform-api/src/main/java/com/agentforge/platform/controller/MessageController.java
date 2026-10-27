package com.agentforge.platform.controller;

import com.agentforge.platform.common.Result;
import com.agentforge.platform.dto.MessageResponse;
import com.agentforge.platform.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 消息接口。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 查询某个会话下的历史消息。
     *
     * GET /api/conversations/{conversationId}/messages
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public Result<List<MessageResponse>> listByConversation(
            @PathVariable Long conversationId) {
        return Result.success(messageService.listByConversation(conversationId));
    }
}