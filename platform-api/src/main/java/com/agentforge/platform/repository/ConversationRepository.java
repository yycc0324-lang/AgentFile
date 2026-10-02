package com.agentforge.platform.repository;

import com.agentforge.platform.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 会话数据访问层。
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
