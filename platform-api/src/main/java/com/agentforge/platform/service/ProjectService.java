package com.agentforge.platform.service;

import com.agentforge.platform.dto.ProjectCreateRequest;
import com.agentforge.platform.dto.ProjectResponse;
import com.agentforge.platform.entity.Project;
import com.agentforge.platform.exception.BusinessException;
import com.agentforge.platform.repository.ProjectRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 项目业务逻辑。
 */
@Service
@AllArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    /**
     * 创建项目。
     */
    @Transactional
    public ProjectResponse create(ProjectCreateRequest request) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setRepoUrl(request.getRepoUrl());
        project.setBlogPath(request.getBlogPath());

        Project saved = projectRepository.save(project);
        return ProjectResponse.from(saved);
    }

    /**
     * 查询项目列表。
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projectRepository.findAll()
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    /**
     * 根据 ID 查询项目。
     */
    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40400, "项目不存在"));
        return ProjectResponse.from(project);
    }
}
