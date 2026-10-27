package com.agentforge.platform.client;

import com.agentforge.platform.dto.AgentRunRequest;
import com.agentforge.platform.dto.AgentRunResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
                // JDK 默认 HttpClient 会尝试 HTTP/2 升级，并可能使用 chunked 传输；
                // 这里使用 SimpleClientHttpRequestFactory，走传统 HTTP/1.1 + Content-Length，
                // 避免 FastAPI/Uvicorn 收到空请求体。
                .requestFactory(new SimpleClientHttpRequestFactory())
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
