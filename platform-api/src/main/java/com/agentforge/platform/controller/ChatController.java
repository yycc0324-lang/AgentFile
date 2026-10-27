package com.agentforge.platform.controller;

import com.agentforge.platform.common.Result;
import com.agentforge.platform.dto.AgentStep;
import com.agentforge.platform.dto.ChatRequest;
import com.agentforge.platform.dto.ChatResponse;
import com.agentforge.platform.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 聊天接口。
 *
 * <p>提供两个版本：</p>
 * <ul>
 *     <li>POST /api/chat        -> 普通 JSON，方便调试</li>
 *     <li>POST /api/chat/stream -> SSE 流式返回</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 普通 JSON 版本。
     */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return Result.success(chatService.chat(request));
    }

    /**
     * SSE 流式版本。
     *
     * <p>当前 M1：Java 先调用 Python 拿到完整结果，再把真实 route / steps
     * 拆成 SSE 事件推送给前端。后续 M2 可以升级为 Python 真正流式返回。</p>
     */
    @PostMapping(
            value = "/chat/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);

        CompletableFuture.runAsync(() -> {
            try {
                ChatResponse response = chatService.chat(request);

                // 1. 推送真实 route 事件
                emitter.send(SseEmitter.event()
                        .name("route")
                        .data(Map.of(
                                "agent",
                                response.getAgent() == null ? "unknown" : response.getAgent()
                        )));

                // 2. 推送 Python 返回的真实 steps
                List<AgentStep> steps = response.getSteps() == null
                        ? List.of()
                        : response.getSteps();

                for (AgentStep step : steps) {
                    // route 已经通过 route 事件单独推送，这里跳过，避免时间线重复。
                    if ("route".equals(step.getType())) {
                        continue;
                    }

                    Map<String, Object> stepData = new HashMap<>();
                    stepData.put("type", step.getType());
                    stepData.put("tool", step.getTool());
                    stepData.put("args", step.getArgs());
                    stepData.put("status", step.getStatus());
                    stepData.put("resultCount", step.getResultCount());

                    emitter.send(SseEmitter.event()
                            .name("step")
                            .data(stepData));
                }

                // 3. 推送 answer 事件
                Map<String, Object> answerData = new HashMap<>();
                answerData.put("content", response.getContent());
                answerData.put(
                        "citations",
                        response.getCitations() == null
                                ? List.of()
                                : response.getCitations()
                );

                emitter.send(SseEmitter.event()
                        .name("answer")
                        .data(answerData));

                // 4. 推送 done 事件
                Map<String, Object> usage = new HashMap<>();
                usage.put("promptTokens", response.getPromptTokens());
                usage.put("completionTokens", response.getCompletionTokens());
                usage.put("totalTokens", response.getTotalTokens());
                usage.put("latencyMs", response.getLatencyMs());

                Map<String, Object> doneData = new HashMap<>();
                doneData.put("traceId", response.getTraceId());
                doneData.put("usage", usage);

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(doneData));

                emitter.complete();

            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of(
                                    "message",
                                    e.getMessage() == null
                                            ? "Agent 执行失败"
                                            : e.getMessage()
                            )));
                } catch (IOException ignored) {
                    // 客户端可能已经断开，忽略
                }

                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
