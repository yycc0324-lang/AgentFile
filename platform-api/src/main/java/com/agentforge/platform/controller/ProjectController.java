package com.agentforge.platform.controller;

import com.agentforge.platform.common.Result;
import com.agentforge.platform.dto.ProjectCreateRequest;
import com.agentforge.platform.dto.ProjectResponse;
import com.agentforge.platform.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目接口。
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * 创建项目。
     *
     * POST /api/projects
     */
    @PostMapping
    public Result<ProjectResponse> create(@Valid @RequestBody ProjectCreateRequest request) {
        return Result.success(projectService.create(request));
    }

    /**
     * 查询项目列表。
     *
     * GET /api/projects
     */
    @GetMapping
    public Result<List<ProjectResponse>> list() {
        return Result.success(projectService.list());
    }

    /**
     * 查询项目详情。
     *
     * GET /api/projects/{id}
     */
    @GetMapping("/{id}")
    public Result<ProjectResponse> getById(@PathVariable Long id) {
        return Result.success(projectService.getById(id));
    }
}
