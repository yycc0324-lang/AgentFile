package com.agentforge.platform.service;

import com.agentforge.platform.dto.MessageResponse;
import com.agentforge.platform.exception.BusinessException;
import com.agentforge.platform.repository.ConversationRepository;
import com.agentforge.platform.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 消息查询业务。
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    /**
     * 查询某个会话下的全部消息，按创建时间正序。
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> listByConversation(Long conversationId) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new BusinessException(40400, "会话不存在");
        }

        return messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(MessageResponse::from)
                .toList();
    }
}