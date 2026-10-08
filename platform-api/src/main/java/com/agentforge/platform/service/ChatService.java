package com.agentforge.platform.service;

import com.agentforge.platform.client.AgentRuntimeClient;
import com.agentforge.platform.dto.AgentRunRequest;
import com.agentforge.platform.dto.AgentRunResponse;
import com.agentforge.platform.dto.ChatRequest;
import com.agentforge.platform.dto.ChatResponse;
import com.agentforge.platform.entity.Conversation;
import com.agentforge.platform.entity.Message;
import com.agentforge.platform.exception.BusinessException;
import com.agentforge.platform.repository.ConversationRepository;
import com.agentforge.platform.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 聊天业务逻辑。
 *
 * 负责把 Java 平台和 Python Agent Runtime 串起来。
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AgentRuntimeClient agentRuntimeClient;

    /**
     * 聊天主流程。
     *
     * 1. 校验会话
     * 2. 保存用户消息
     * 3. 调用 Python Agent
     * 4. 保存助手消息
     * 5. 返回结果
     */
    @Transactional
    public ChatResponse chat(ChatRequest request) {

        // ==============================
        // 1. 校验会话是否存在
        // ==============================
        Conversation conversation = conversationRepository
                .findById(request.getConversationId())
                .orElseThrow(() -> new BusinessException(40400, "会话不存在"));

        // 校验会话是否属于当前项目，防止跨项目访问。
        if (!conversation.getProjectId().equals(request.getProjectId())) {
            throw new BusinessException(40000, "会话不属于该项目");
        }

        // ==============================
        // 2. 保存用户消息
        // ==============================
        Message userMessage = new Message();
        userMessage.setConversationId(conversation.getId());
        userMessage.setRole("user");
        userMessage.setContent(request.getMessage());
        userMessage.setStatus("success");

        userMessage = messageRepository.save(userMessage);

        // ==============================
        // 3. 组装 Python 请求
        // ==============================
        AgentRunRequest agentRequest = AgentRunRequest.builder()
                // 当前先用 UUID 模拟任务 ID。
                // 后面创建 task 表实体后，这里换成真实的 task.id。
                .taskId("task-" + UUID.randomUUID())
                .projectId(request.getProjectId())
                .conversationId(request.getConversationId())
                .messageId(userMessage.getId())
                .query(request.getMessage())
                .showCitations(Boolean.TRUE.equals(request.getShowCitations()))
                .stream(false)
                .build();

        // ==============================
        // 4. 调用 Python /agent/run
        // ==============================
        long start = System.currentTimeMillis();

        AgentRunResponse agentResponse;
        try {
            agentResponse = agentRuntimeClient.run(agentRequest);
        } catch (Exception e) {
            // Python 服务不可用、超时、返回错误等
            throw new BusinessException(50000, "Agent Runtime 调用失败: " + e.getMessage());
        }

        int latencyMs = (int) (System.currentTimeMillis() - start);

        // ==============================
        // 5. 解析 token 用量
        // ==============================
        int promptTokens = getUsage(agentResponse, "prompt_tokens");
        int completionTokens = getUsage(agentResponse, "completion_tokens");
        int totalTokens = getUsage(agentResponse, "total_tokens");

        // 如果 Python 没返回 total_tokens，就算一下。
        if (totalTokens == 0) {
            totalTokens = promptTokens + completionTokens;
        }

        // ==============================
        // 6. 保存助手消息
        // ==============================
        Message assistantMessage = new Message();
        assistantMessage.setConversationId(conversation.getId());
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(agentResponse.getContent());
        assistantMessage.setPromptTokens(promptTokens);
        assistantMessage.setCompletionTokens(completionTokens);
        assistantMessage.setTotalTokens(totalTokens);
        assistantMessage.setLatencyMs(latencyMs);
        assistantMessage.setStatus("success");

        assistantMessage = messageRepository.save(assistantMessage);

        // ==============================
        // 7. 返回前端需要的聚合结果
        // ==============================
        return ChatResponse.builder()
                .userMessageId(userMessage.getId())
                .assistantMessageId(assistantMessage.getId())
                .content(agentResponse.getContent())
                .citations(agentResponse.getCitations())
                .traceId(agentResponse.getTraceId())
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .latencyMs(latencyMs)
                .build();
    }

    /**
     * 从 Python 返回的 usage Map 中安全读取 token 数。
     */
    private int getUsage(AgentRunResponse response, String key) {
        if (response.getUsage() == null) {
            return 0;
        }
        return response.getUsage().getOrDefault(key, 0);
    }
}