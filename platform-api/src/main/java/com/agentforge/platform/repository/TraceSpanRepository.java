package com.agentforge.platform.repository;

import com.agentforge.platform.entity.TraceSpan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TraceSpanRepository extends JpaRepository<TraceSpan, Long> {

    List<TraceSpan> findByTraceIdOrderByStartedAtAsc(String traceId);
}