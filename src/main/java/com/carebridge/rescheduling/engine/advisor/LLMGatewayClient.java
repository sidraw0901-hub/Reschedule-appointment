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
    private final String model;
    private final String product;
    private final String cookie;
    private final long timeoutMs;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LLMGatewayClient(
            @Value("${carebridge.llm.gateway-url:https://litellm-qc.zycus.net/v1/chat/completions}") String gatewayUrl,
            @Value("${carebridge.llm.api-key:sk-SfyNGxhcv7RnQKbZFWX2LQ}") String apiKey,
            @Value("${carebridge.llm.model:qwen-cursor}") String model,
            @Value("${carebridge.llm.product:PC1}") String product,
            @Value("${carebridge.llm.cookie:6bf6da0e46dc446bd58693d49c303e18=f3f865650f0f8f3b30731936b2eb5857}") String cookie,
            @Value("${carebridge.llm.timeout-ms:15000}") long timeoutMs,
            ObjectMapper objectMapper) {
        this.gatewayUrl = gatewayUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.product = product;
        this.cookie = cookie;
        this.timeoutMs = timeoutMs;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(timeoutMs))
            .build();
    }

    public String callLLMGateway(String systemPrompt, String userPrompt) throws Exception {
        Map<String, Object> requestPayload = Map.of(
            "model", model,
            "messages", java.util.List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "response_format", Map.of("type", "json_object"),
            "temperature", 0.2
        );

        String jsonBody = objectMapper.writeValueAsString(requestPayload);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(gatewayUrl))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMillis(timeoutMs))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (apiKey != null && !apiKey.isBlank()) {
            requestBuilder.header("Authorization", apiKey.startsWith("Bearer ") ? apiKey : "Bearer " + apiKey);
        }
        if (product != null && !product.isBlank()) {
            requestBuilder.header("product", product);
        }
        if (cookie != null && !cookie.isBlank()) {
            requestBuilder.header("Cookie", cookie);
        }

        HttpRequest request = requestBuilder.build();

        log.info("Sending reschedule optimization prompt to LLM Gateway [{}] using model [{}]...", gatewayUrl, model);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            log.info("LLM Gateway responded with status [{}]", response.statusCode());
            return response.body();
        } else {
            String errorMsg = "LLM Gateway returned HTTP status " + response.statusCode() + ": " + response.body();
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }
}
