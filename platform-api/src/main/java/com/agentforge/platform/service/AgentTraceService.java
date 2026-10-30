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

import java.time.LocalDateTime;
import java.util.List;

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

        int index = 1;

        for (AgentStep step : steps) {
            String stepName = step.getTool() == null
                    ? step.getType()
                    : step.getTool();

            TaskStep taskStep = new TaskStep();
            taskStep.setTaskId(task.getId());
            taskStep.setStepIndex(index);
            taskStep.setStepType(step.getType());
            taskStep.setName(stepName);
            taskStep.setStatus(step.getStatus() == null ? "success" : step.getStatus());
            taskStep.setInput(toJson(step.getArgs()));
            taskStep.setOutput(buildOutput(step));
            taskStep.setStartedAt(LocalDateTime.now());
            taskStep.setEndedAt(LocalDateTime.now());
            taskStep.setDurationMs(0);
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
                toolCall.setDurationMs(0);
                toolCall.setCalledAt(LocalDateTime.now());
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
            span.setStartedAt(LocalDateTime.now());
            span.setEndedAt(LocalDateTime.now());
            span.setDurationMs(0);
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

        TraceResponse response = new TraceResponse();
        response.setTraceId(traceId);
        response.setStatus(
                spans.stream().anyMatch(s -> "failed".equalsIgnoreCase(s.getStatus()))
                        ? "failed"
                        : "success"
        );
        response.setDurationMs(
                spans.stream()
                        .map(TraceSpan::getDurationMs)
                        .filter(d -> d != null)
                        .mapToInt(Integer::intValue)
                        .sum()
        );
        response.setSpanCount(spans.size());
        response.setTokens(0);
        response.setSpans(spans.stream().map(this::toTraceSpanResponse).toList());
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