package com.carebridge.rescheduling.engine.advisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class LLMGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(LLMGatewayClient.class);

    private final String gatewayUrl;
    private final String apiKey;
    private final long timeoutMs;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LLMGatewayClient(
            @Value("${carebridge.llm.gateway-url:http://localhost:8080/mock-llm}") String gatewayUrl,
            @Value("${carebridge.llm.api-key:mock-key}") String apiKey,
            @Value("${carebridge.llm.timeout-ms:3000}") long timeoutMs,
            ObjectMapper objectMapper) {
        this.gatewayUrl = gatewayUrl;
        this.apiKey = apiKey;
        this.timeoutMs = timeoutMs;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(timeoutMs))
            .build();
    }

    public String callLLMGateway(String systemPrompt, String userPrompt) throws Exception {
        Map<String, Object> requestPayload = Map.of(
            "model", "gpt-4o-mini",
            "messages", java.util.List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "response_format", Map.of("type", "json_object"),
            "temperature", 0.2
        );

        String jsonBody = objectMapper.writeValueAsString(requestPayload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(gatewayUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .timeout(Duration.ofMillis(timeoutMs))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        log.debug("Calling LLM Gateway at [{}] with timeout [{}ms]", gatewayUrl, timeoutMs);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            throw new RuntimeException("LLM Gateway returned HTTP status " + response.statusCode() + ": " + response.body());
        }
    }
}
