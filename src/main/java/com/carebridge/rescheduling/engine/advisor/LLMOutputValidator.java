package com.carebridge.rescheduling.engine.advisor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Component
public class LLMOutputValidator {

    private final ObjectMapper objectMapper;

    public LLMOutputValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ValidatedLLMOutput validate(String rawResponse, Set<UUID> allowedSlotIds) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);

            if (!root.hasNonNull("recommendedSlotId")) {
                throw new IllegalArgumentException("Missing recommendedSlotId in LLM output");
            }

            UUID slotId = UUID.fromString(root.get("recommendedSlotId").asText());

            // Non-negotiable security guardrail: Check against allowlist
            if (!allowedSlotIds.contains(slotId)) {
                throw new IllegalArgumentException("Hallucination detected: slot " + slotId + " is not in the candidate allowlist");
            }

            UUID altProviderId = null;
            if (root.hasNonNull("alternateProviderId") && !root.get("alternateProviderId").asText().equalsIgnoreCase("null")) {
                altProviderId = UUID.fromString(root.get("alternateProviderId").asText());
            }

            BigDecimal confidence = new BigDecimal("0.90");
            if (root.hasNonNull("confidenceScore")) {
                double confDouble = root.get("confidenceScore").asDouble();
                if (confDouble >= 0.0 && confDouble <= 1.0) {
                    confidence = BigDecimal.valueOf(confDouble);
                }
            }

            String script = root.hasNonNull("patientFacingScript") ? root.get("patientFacingScript").asText() : "";
            String rationale = root.hasNonNull("clinicalRationale") ? root.get("clinicalRationale").asText() : "";

            String fullExplanation = script + " [AI Rationale: " + rationale + "]";

            return new ValidatedLLMOutput(slotId, altProviderId, confidence, fullExplanation);
        } catch (Exception e) {
            throw new RuntimeException("LLM Output Validation Failed: " + e.getMessage(), e);
        }
    }

    public static class ValidatedLLMOutput {
        private final UUID slotId;
        private final UUID alternateProviderId;
        private final BigDecimal confidence;
        private final String explanation;

        public ValidatedLLMOutput(UUID slotId, UUID alternateProviderId, BigDecimal confidence, String explanation) {
            this.slotId = slotId;
            this.alternateProviderId = alternateProviderId;
            this.confidence = confidence;
            this.explanation = explanation;
        }

        public UUID getSlotId() { return slotId; }
        public UUID getAlternateProviderId() { return alternateProviderId; }
        public BigDecimal getConfidence() { return confidence; }
        public String getExplanation() { return explanation; }
    }
}
