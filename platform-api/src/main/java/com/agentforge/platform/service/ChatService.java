package com.agentforge.platform.service;

import com.agentforge.platform.client.AgentRuntimeClient;
import com.agentforge.platform.dto.AgentRunRequest;
import com.agentforge.platform.dto.AgentRunResponse;
import com.agentforge.platform.dto.ChatRequest;
import com.agentforge.platform.dto.ChatResponse;
import com.agentforge.platform.entity.Conversation;
import com.agentforge.platform.entity.Message;
import com.agentforge.platform.entity.Project;
import com.agentforge.platform.entity.Task;
import com.agentforge.platform.exception.BusinessException;
import com.agentforge.platform.repository.ConversationRepository;
import com.agentforge.platform.repository.MessageRepository;
import com.agentforge.platform.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 聊天业务逻辑。
 *
 * 负责把 Java 平台和 Python Agent Runtime 串起来。
 */
//类声明与依赖注入
// @RequiredArgsConstructor：Lombok 注解，为所有 final 字段生成构造函数。Spring 看到构造函数，就会自动把四个 Repository 和 ObjectMapper 注入进来（构造器注入，比 @Autowired 字段注入更好，能避免循环依赖）
@Service
@RequiredArgsConstructor

/*
*
* @NoArgsConstructor	OrderService()	0 个
@AllArgsConstructor	OrderService(UserRepository, OrderRepository, String, int)	全部字段
@RequiredArgsConstructor	OrderService(UserRepository, OrderRepository)	仅 final 且未初始化的字段
这里不使用No构造函数和All函数是因为一个生成的构造函数是0，另一个构造了所有的字段（No/All），然后RequiredArgsConstructor是
* 仅仅初始化final字段的
* */

@Slf4j//自动生成日志对象
public class ChatService {

    private final ConversationRepository conversationRepository;//会话表
    private final MessageRepository messageRepository;//消息表
    private final AgentRuntimeClient agentRuntimeClient;//Agent client服务
    private final ProjectRepository projectRepository; //项目表
    private final AgentTraceService agentTraceService;//进度表/状态表
    /**
     * 聊天主流程。
     *
     * 1. 校验会话
     * 2. 保存用户消息
     * 3. 调用 Python Agent
     * 4. 保存助手消息
     * 5. 返回结果
     */
   // @Transactional 它告诉 Spring：执行这个方法时，帮我开一个数据库事务（理解为监听吧）；方法正常结束就提交，抛异常就回滚。
    //但是这个注解会监听事物的时候占用链接数，结果返回里面含有模型推理太慢了
    public ChatResponse chat(ChatRequest request) {

        // ==============================
        // 1. 校验会话是否存在
        // ==============================
        Conversation conversation = conversationRepository//初始化会话表

                .findById(request.getConversationId())
                .orElseThrow(() -> new BusinessException(40400, "会话不存在"));

        // 校验会话是否属于当前项目，防止跨项目访问。
        if (!conversation.getProjectId().equals(request.getProjectId())) {
            throw new BusinessException(40000, "会话不属于该项目");
        }

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new BusinessException(40400, "项目不存在"));
        //上面是只校验前端数字是否对的上，这里是校验项目是否存在，当你删掉项目的时候继续追问可能会遇到


        // ==============================
        // 2. 保存用户消息，使用JPA TODO 之后该成MyBatis plus +My SQL/Pg SQL
        // ==============================
        Message userMessage = new Message();
        userMessage.setConversationId(conversation.getId());
        userMessage.setRole("user");
        userMessage.setContent(request.getMessage());
        userMessage.setStatus("success");

        userMessage = messageRepository.save(userMessage);//保存用户信息

        Task task = agentTraceService.createTask(//开一条“执行记录”，status=running，还没有答案
                request.getProjectId(),
                conversation.getId(),
                userMessage.getId(),
                request.getMessage()
        );
        String traceId = "trace-" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 12);

        // ==============================
        // 3. 组装 Python 请求，提前到Py中构造结构
        // ==============================


        AgentRunRequest agentRequest = AgentRunRequest.builder()
                .taskId(String.valueOf(task.getId()))//Python 请求里使用真实 task id
                .projectId(request.getProjectId())
                .conversationId(request.getConversationId())
                .messageId(userMessage.getId())
                .query(request.getMessage())
                .showCitations(Boolean.TRUE.equals(request.getShowCitations()))
                .stream(false)
                .repoUrl(project.getRepoUrl())
                .traceId(traceId)

                .build();

        // ==============================
        // 4. 调用 Python /agent/run
        // ==============================
        long start = System.currentTimeMillis();//Agent开始调用时间

        AgentRunResponse agentResponse;
        try {
            agentResponse = agentRuntimeClient.run(agentRequest);
        } catch (Exception e) {
            // Python 服务不可用、超时、返回错误等
            int latencyMs = (int) (System.currentTimeMillis() - start);

            // 先把失败写进 task + trace，再抛给前端
            try {
                agentTraceService.failTask(task, traceId, e.getMessage(), latencyMs);
            } catch (Exception logError) {
                // 记失败时又失败，别把原始错误盖掉
                log.error("记录失败任务时又出错", logError);
            }

            throw new BusinessException(50000, "Agent Runtime 调用失败: " + e.getMessage());
        }

        // 调用成功：计算端到端耗时
        // 失败路径的耗时在 catch 里单独算过，用于 failTask，两者不冲突（作用域不同）
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

        agentTraceService.completeTask(
                task,
                agentResponse,
                assistantMessage.getId(),
                latencyMs
        );

        // ==============================
        // 7. 返回前端需要的聚合结果
        // ==============================
        return ChatResponse.builder()
                .userMessageId(userMessage.getId())
                .assistantMessageId(assistantMessage.getId())
                .content(agentResponse.getContent())
                .citations(agentResponse.getCitations())
                .traceId(agentResponse.getTraceId())
                .agent(agentResponse.getAgent())
                .steps(agentResponse.getSteps())
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