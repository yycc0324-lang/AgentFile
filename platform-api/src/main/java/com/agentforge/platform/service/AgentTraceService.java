package com.agentforge.platform.service;

import com.agentforge.platform.dto.AgentStep;
import com.agentforge.platform.dto.AgentRunResponse;
import com.agentforge.platform.dto.TaskStepResponse;
import com.agentforge.platform.dto.TraceResponse;
import com.agentforge.platform.dto.TraceSpanResponse;
import com.agentforge.platform.entity.Task;
import com.agentforge.platform.entity.TaskStep;
import com.agentforge.platform.entity.ToolCall;
import com.agentforge.platform.entity.TraceSpan;
import com.agentforge.platform.exception.BusinessException;
import com.agentforge.platform.repository.TaskRepository;
import com.agentforge.platform.repository.TaskStepRepository;
import com.agentforge.platform.repository.ToolCallRepository;
import com.agentforge.platform.repository.TraceSpanRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.agentforge.platform.repository.MessageRepository;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责 Task / TaskStep / ToolCall / TraceSpan 落库与查询。
 */
@Service
@RequiredArgsConstructor
public class AgentTraceService {

    private final TaskRepository taskRepository;
    private final TaskStepRepository taskStepRepository;
    private final ToolCallRepository toolCallRepository;
    private final TraceSpanRepository traceSpanRepository;
    private final ObjectMapper objectMapper;
    private final MessageRepository messageRepository;
    /**
     * 调用 Python 前创建 task。
     */
    @Transactional
    public Task createTask(
            Long projectId,
            Long conversationId,
            Long triggerMessageId,
            String input) {

        Task task = new Task();
        task.setProjectId(projectId);
        task.setConversationId(conversationId);
        task.setTriggerMessageId(triggerMessageId);
        task.setTaskType("chat");
        task.setTitle(input != null && input.length() > 200
                ? input.substring(0, 200)
                : input);
        task.setInput(input);
        task.setStatus("running");
        task.setMaxSteps(20);
        task.setCurrentStep(0);
        task.setStartedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    /**
     * Python 成功返回后保存 task / steps / tool_call / trace_span。
     */
    @Transactional
    public void completeTask(
            Task task,
            AgentRunResponse response,
            Long assistantMessageId,
            int latencyMs) {

        List<AgentStep> steps = response.getSteps() == null
                ? List.of()
                : response.getSteps();

        task.setStatus("success");
        task.setResult(response.getContent());
        task.setCurrentStep(steps.size());
        task.setFinishedAt(LocalDateTime.now());
        taskRepository.save(task);

        // 工具步骤耗时之和；剩余时间算模型思考，归到 answer 那一步上
        int toolMillis = steps.stream()
                .filter(s -> "tool_call".equals(s.getType()))
                .mapToInt(s -> s.getDurationMs() == null ? 0 : s.getDurationMs())
                .sum();

        LocalDateTime rootStartedAt = task.getStartedAt() == null
                ? LocalDateTime.now()
                : task.getStartedAt();

        // 根 span：一次 Agent 执行的总体，所有子 span 都挂在它下面
        TraceSpan root = new TraceSpan();
        root.setTraceId(response.getTraceId());
        root.setConversationId(task.getConversationId());
        root.setMessageId(assistantMessageId);
        root.setTaskId(task.getId());
        root.setName("agent.run");
        root.setSpanType("agent");
        root.setStatus("success");
        root.setInput(task.getInput());
        root.setStartedAt(rootStartedAt);
        root.setEndedAt(rootStartedAt.plusNanos(latencyMs * 1_000_000L));
        root.setDurationMs(latencyMs);
        root = traceSpanRepository.save(root);

        // 子 span 按步骤顺序排成一条时间轴，总和等于 latencyMs
        LocalDateTime cursor = rootStartedAt;
        int index = 1;

        for (AgentStep step : steps) {
            String stepName = step.getTool() == null
                    ? step.getType()
                    : step.getTool();

            int durationMs = step.getDurationMs() == null ? 0 : step.getDurationMs();
            if ("answer".equals(step.getType())) {
                // Python 的 answer.duration_ms 是整轮耗时，这里换成"扣掉工具后的剩余时间"
                durationMs = Math.max(0, latencyMs - toolMillis);
            }
            LocalDateTime startedAt = cursor;
            LocalDateTime endedAt = cursor.plusNanos(durationMs * 1_000_000L);
            cursor = endedAt;

            TaskStep taskStep = new TaskStep();
            taskStep.setTaskId(task.getId());
            taskStep.setStepIndex(index);
            taskStep.setStepType(step.getType());
            taskStep.setName(stepName);
            taskStep.setStatus(step.getStatus() == null ? "success" : step.getStatus());
            taskStep.setInput(toJson(step.getArgs()));
            taskStep.setOutput(buildOutput(step));
            taskStep.setStartedAt(startedAt);
            taskStep.setEndedAt(endedAt);
            taskStep.setDurationMs(durationMs);
            taskStep = taskStepRepository.save(taskStep);

            Long toolCallId = null;

            if ("tool_call".equals(step.getType())) {
                ToolCall toolCall = new ToolCall();
                toolCall.setTaskId(task.getId());
                toolCall.setStepId(taskStep.getId());
                toolCall.setTraceId(response.getTraceId());
                toolCall.setToolName(step.getTool());
                toolCall.setArguments(toJson(step.getArgs()));
                toolCall.setResult(buildOutput(step));
                toolCall.setStatus(step.getStatus() == null ? "success" : step.getStatus());
                toolCall.setDurationMs(durationMs);
                toolCall.setCalledAt(startedAt);
                toolCall = toolCallRepository.save(toolCall);
                toolCallId = toolCall.getId();
            }

            TraceSpan span = new TraceSpan();
            span.setTraceId(response.getTraceId());
            span.setConversationId(task.getConversationId());
            span.setMessageId(assistantMessageId);
            span.setTaskId(task.getId());
            span.setStepId(taskStep.getId());
            span.setToolCallId(toolCallId);
            span.setName(stepName);
            span.setSpanType(mapSpanType(step.getType()));
            span.setStatus(step.getStatus() == null ? "success" : step.getStatus());
            span.setInput(toJson(step.getArgs()));
            span.setOutput(buildOutput(step));
            span.setStartedAt(startedAt);
            span.setEndedAt(endedAt);
            span.setDurationMs(durationMs);
            span.setParentSpanId(root.getId());
            traceSpanRepository.save(span);

            index++;
        }
    }

    /**
     * Python 调用失败时落库：task 标记 failed，并写一条 failed 的根 span。
     *
     * <p>必须独立事务提交，否则会被调用方的事务回滚掉。</p>
     */
    @Transactional
    public void failTask(Task task, String traceId, String errorMessage, int latencyMs) {
        LocalDateTime now = LocalDateTime.now();
        String message = truncate(errorMessage, 2000);

        task.setStatus("failed");
        task.setErrorMessage(message);
        task.setFinishedAt(now);
        taskRepository.save(task);

        if (traceId == null || traceId.isBlank()) {
            return;
        }

        TraceSpan span = new TraceSpan();
        span.setTraceId(traceId);
        span.setConversationId(task.getConversationId());
        span.setTaskId(task.getId());
        span.setName("agent.run");
        span.setSpanType("agent");
        span.setStatus("failed");
        span.setInput(task.getInput());
        span.setErrorMessage(message);
        span.setStartedAt(task.getStartedAt() == null ? now : task.getStartedAt());
        span.setEndedAt(now);
        span.setDurationMs(latencyMs);
        traceSpanRepository.save(span);
    }

    /**
     * 查询 Trace。
     */
    @Transactional(readOnly = true)
    public TraceResponse getTrace(String traceId) {
        List<TraceSpan> spans = traceSpanRepository
                .findByTraceIdOrderByStartedAtAsc(traceId);

        if (spans.isEmpty()) {
            throw new BusinessException(40400, "Trace 不存在");
        }

        // 先全部转成响应对象，再按 parentSpanId 挂成树
        Map<Long, TraceSpanResponse> byId = new LinkedHashMap<>();
        for (TraceSpan span : spans) {
            byId.put(span.getId(), toTraceSpanResponse(span));
        }

        List<TraceSpanResponse> roots = new ArrayList<>();
        for (TraceSpan span : spans) {
            TraceSpanResponse item = byId.get(span.getId());
            Long parentId = span.getParentSpanId();
            if (parentId != null && byId.containsKey(parentId)) {
                byId.get(parentId).getChildren().add(item);
            } else {
                roots.add(item);
            }
        }
        // 顺着 span 上的 message_id 去 message 表拿这一次的 token 总量
        int tokens = 0;
        for (TraceSpan span : spans) {
            if (span.getMessageId() != null) {
                tokens = messageRepository.findById(span.getMessageId())
                        .map(m -> m.getTotalTokens() == null ? 0 : m.getTotalTokens())
                        .orElse(0);
                break;
            }
        }

        TraceResponse response = new TraceResponse();
        response.setTraceId(traceId);
        response.setStatus(
                spans.stream().anyMatch(s -> "failed".equalsIgnoreCase(s.getStatus()))
                        ? "failed"
                        : "success"
        );
        // 总耗时用根节点，不能把根和子节点再加一遍
        response.setDurationMs(roots.isEmpty() ? 0 : roots.get(0).getDurationMs());
        response.setSpanCount(spans.size());
        response.setTokens(tokens);
        response.setSpans(roots);
        return response;
    }

    /**
     * 查询某个 task 的步骤。
     */
    @Transactional(readOnly = true)
    public List<TaskStepResponse> listSteps(Long taskId) {
        return taskStepRepository.findByTaskIdOrderByStepIndexAsc(taskId)
                .stream()
                .map(this::toTaskStepResponse)
                .toList();
    }

    private TraceSpanResponse toTraceSpanResponse(TraceSpan span) {
        TraceSpanResponse item = new TraceSpanResponse();
        item.setId(span.getId());
        item.setName(span.getName());
        item.setType(span.getSpanType());
        item.setStatus(span.getStatus());
        item.setDurationMs(span.getDurationMs());
        item.setInput(span.getInput());
        item.setOutput(span.getOutput());
        item.setError(span.getErrorMessage());
        return item;
    }

    private TaskStepResponse toTaskStepResponse(TaskStep step) {
        TaskStepResponse item = new TaskStepResponse();
        item.setId(step.getId());
        item.setTaskId(step.getTaskId());
        item.setStepIndex(step.getStepIndex());
        item.setStepType(step.getStepType());
        item.setName(step.getName());
        item.setStatus(step.getStatus());
        item.setInput(step.getInput());
        item.setOutput(step.getOutput());
        item.setDurationMs(step.getDurationMs());
        item.setStartedAt(step.getStartedAt());
        item.setEndedAt(step.getEndedAt());
        return item;
    }

    private String mapSpanType(String stepType) {
        if ("tool_call".equals(stepType)) {
            return "tool";
        }
        if ("route".equals(stepType)) {
            return "agent";
        }
        return "agent";
    }

    private String buildOutput(AgentStep step) {
        if (step.getResultCount() == null) {
            return null;
        }
        return "{\"result_count\":" + step.getResultCount() + "}";
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private String truncate(String text, int limit) {
        if (text == null || text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit);
    }
}