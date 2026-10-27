package com.agentforge.platform.repository;

import com.agentforge.platform.entity.TaskStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskStepRepository extends JpaRepository<TaskStep, Long> {

    List<TaskStep> findByTaskIdOrderByStepIndexAsc(Long taskId);
}