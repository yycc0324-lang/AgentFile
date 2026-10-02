package com.agentforge.platform.dto;

import com.agentforge.platform.entity.Project;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目响应。
 */
@Data
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private String repoUrl;
    private String blogPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse from(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setRepoUrl(project.getRepoUrl());
        response.setBlogPath(project.getBlogPath());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());
        return response;
    }
}
