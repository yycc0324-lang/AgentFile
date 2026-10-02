package com.agentforge.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建项目请求。
 */
@Data
public class ProjectCreateRequest {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 255, message = "项目名称不能超过255个字符")
    private String name;

    private String description;

    private String repoUrl;

    private String blogPath;
}
