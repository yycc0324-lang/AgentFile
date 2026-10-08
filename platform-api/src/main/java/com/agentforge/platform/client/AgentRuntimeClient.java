package com.agentforge.platform.client;

import com.agentforge.platform.dto.AgentRunRequest;
import com.agentforge.platform.dto.AgentRunResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Java 调用 Python Agent Runtime 的 HTTP 客户端。
 */
@Component
public class AgentRuntimeClient {

    private final RestClient restClient;

    public AgentRuntimeClient(
            @Value("${agent.runtime.base-url:http://127.0.0.1:8000}")
            String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public AgentRunResponse run(AgentRunRequest request) {
        return restClient.post()
                .uri("/agent/run")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AgentRunResponse.class);
    }
}
