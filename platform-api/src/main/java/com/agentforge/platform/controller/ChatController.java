package com.agentforge.platform.controller;

import com.agentforge.platform.common.Result;
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

 * 提供两个版本：
 *     POST /api/chat        -> 普通 JSON，方便调试</li>
 *     POST /api/chat/stream -> SSE 流式返回</li>
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
     * <p>M1 第一版：Java 先同步调用 Python，拿到完整结果后，
     * 再拆成 SSE 事件推给前端。</p>
     *
     * <p>后续 M2 可以升级为 Python 真正流式返回 NDJSON，
     * Java 再实时透传 event。</p>
     */
    @PostMapping(
            value = "/chat/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE //这里是告诉Spring这个接口是SSE流式输出
    )
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        // 超时时间：120 秒自动断开链接
        SseEmitter emitter = new SseEmitter(120_000L);

        // 使用异步线程执行 Agent 调用。
        // 不要阻塞 Tomcat 请求线程。
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 推送 route 事件
                //    前端可以显示：路由到 code_agent
                emitter.send(SseEmitter.event()
                        .name("route")
                        .data(Map.of(
                                "agent", "code_agent"
                        )));

                // 2. 推送 step 事件。
                //    M1 这里先 mock 一个工具调用步骤。
                Map<String, Object> stepData = new HashMap<>();
                stepData.put("type", "tool_call");
                stepData.put("tool", "search_code");
                stepData.put("args", Map.of(
                        "query", request.getMessage()
                ));

                emitter.send(SseEmitter.event()
                        .name("step")
                        .data(stepData));

                // 3. 真正调用 Java ChatService：
                //    - 保存用户消息
                //    - 调用 Python /agent/run
                //    - 保存助手消息
                ChatResponse response = chatService.chat(request);

                // 4. 推送 answer 事件
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

                // 5. 推送 done 事件
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

                // 6. 正常结束
                emitter.complete();

            } catch (Exception e) {
                // 出错时，尽量推送一个 error 事件
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