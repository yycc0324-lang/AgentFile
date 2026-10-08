package com.agentforge.platform.repository;

import com.agentforge.platform.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 消息数据访问层。
 */
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 查询某个会话下的全部消息，按创建时间正序排列。
     *
     * Spring Data JPA 会自动生成类似下面的 SQL：
     *
     * SELECT *
     * FROM message
     * WHERE conversation_id = ?
     * ORDER BY created_at ASC;
     */
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}