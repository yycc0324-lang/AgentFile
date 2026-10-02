package com.agentforge.platform.controller;

import com.agentforge.platform.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * M1 开发期临时验证接口，后续可以删除或保留。
 */
@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("pong");
    }
}
