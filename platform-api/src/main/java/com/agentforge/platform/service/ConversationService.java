package com.agentforge.platform.service;

import com.agentforge.platform.dto.ConversationCreateRequest;
import com.agentforge.platform.dto.ConversationResponse;
import com.agentforge.platform.entity.Conversation;
import com.agentforge.platform.exception.BusinessException;
import com.agentforge.platform.repository.ConversationRepository;
import com.agentforge.platform.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 会话业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ProjectRepository projectRepository;

    /**
     * 创建会话。
     */
    @Transactional
    public ConversationResponse create(ConversationCreateRequest request) {
        if (!projectRepository.existsById(request.getProjectId())) {
            throw new BusinessException(404, "项目不存在");
        }

        Conversation conversation = new Conversation();
        conversation.setProjectId(request.getProjectId());
        conversation.setTitle(request.getTitle());

        return ConversationResponse.from(conversationRepository.save(conversation));
    }

    /**
     * 查询某个项目下的会话列表。
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> listByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new BusinessException(40400, "项目不存在");
        }

        return conversationRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(ConversationResponse::from)
                .toList();
    }
}
