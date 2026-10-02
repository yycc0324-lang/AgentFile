package com.agentforge.platform.repository;

import com.agentforge.platform.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 项目数据访问层。
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {
    /*
    *
    *   Spring Data JPA 会自动帮我们实现：
        save()
        findAll()
        findById()
        deleteById()
...*/
}