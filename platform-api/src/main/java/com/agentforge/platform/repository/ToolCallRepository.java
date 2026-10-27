package com.agentforge.platform.repository;

import com.agentforge.platform.entity.ToolCall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToolCallRepository extends JpaRepository<ToolCall, Long> {

    List<ToolCall> findByTaskIdOrderByCalledAtAsc(Long taskId);
}