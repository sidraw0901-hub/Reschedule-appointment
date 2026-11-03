package com.carebridge.rescheduling.engine.advisor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@CrossOrigin(origins = "*")
public class MockLLMController {

    private final ObjectMapper objectMapper;
    private static final Pattern SLOT_PATTERN = Pattern.compile("Slot ID:\\s*([0-9a-fA-F\\-]+)");

    public MockLLMController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping("/mock-llm")
    public ResponseEntity<Map<String, Object>> mockChatCompletion(@RequestBody Map<String, Object> request) {
        String chosenSlotId = null;
        try {
            @SuppressWarnings("unchecked")
            var messages = (java.util.List<Map<String, String>>) request.get("messages");
            if (messages != null && messages.size() > 1) {
                String userContent = messages.get(1).get("content");
                Matcher m = SLOT_PATTERN.matcher(userContent);
                if (m.find()) {
                    chosenSlotId = m.group(1);
                }
            }
        } catch (Exception ignored) {}

        String jsonResponse = String.format("""
            {
              "recommendedSlotId": "%s",
              "alternateProviderId": null,
              "confidenceScore": 0.94,
              "clinicalRationale": "Same specialty match with minimal schedule displacement and compatible exam room.",
              "patientFacingScript": "Hello! Due to an unforeseen doctor disruption, we have held a priority slot for you today so your care is not delayed. Does this updated time work for you?"
            }
            """, chosenSlotId != null ? chosenSlotId : "00000000-0000-0000-0000-000000000000");

        return ResponseEntity.ok(Map.of(
            "id", "mock-cmpl-" + System.currentTimeMillis(),
            "object", "chat.completion",
            "choices", java.util.List.of(
                Map.of(
                    "message", Map.of(
                        "role", "assistant",
                        "content", jsonResponse
                    )
                )
            )
        ));
    }
}
