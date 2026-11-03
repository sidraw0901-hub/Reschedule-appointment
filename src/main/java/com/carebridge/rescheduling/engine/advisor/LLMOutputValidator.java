package com.carebridge.rescheduling.engine.advisor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Component
public class LLMOutputValidator {

    private static final Logger log = LoggerFactory.getLogger(LLMOutputValidator.class);
    private final ObjectMapper objectMapper;

    public LLMOutputValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ValidatedLLMOutput validate(String rawResponse, Set<UUID> allowedSlotIds) {
        try {
            String clean = rawResponse != null ? rawResponse.trim() : "";
            
            // Strip markdown code fences if present
            if (clean.contains("```")) {
                int firstFence = clean.indexOf("```");
                int secondFence = clean.indexOf("```", firstFence + 3);
                if (secondFence != -1) {
                    String inside = clean.substring(firstFence + 3, secondFence).trim();
                    if (inside.toLowerCase().startsWith("json")) {
                        inside = inside.substring(4).trim();
                    }
                    clean = inside;
                }
            }

            // Extract outermost JSON object if there is conversational preamble
            int firstBrace = clean.indexOf('{');
            int lastBrace = clean.lastIndexOf('}');
            if (firstBrace != -1 && lastBrace > firstBrace) {
                clean = clean.substring(firstBrace, lastBrace + 1);
            }

            log.debug("Parsing cleaned LLM JSON: {}", clean);
            JsonNode root = objectMapper.readTree(clean);

            // Flexible slot ID key resolution
            JsonNode slotNode = root.get("recommendedSlotId");
            if (slotNode == null || slotNode.isNull()) slotNode = root.get("recommended_slot_id");
            if (slotNode == null || slotNode.isNull()) slotNode = root.get("slotId");
            if (slotNode == null || slotNode.isNull()) slotNode = root.get("slot_id");
            if ((slotNode == null || slotNode.isNull()) && root.has("recommendation")) {
                JsonNode recNode = root.get("recommendation");
                if (recNode.hasNonNull("recommendedSlotId")) slotNode = recNode.get("recommendedSlotId");
                else if (recNode.hasNonNull("slotId")) slotNode = recNode.get("slotId");
                else if (recNode.hasNonNull("recommended_slot_id")) slotNode = recNode.get("recommended_slot_id");
            }

            if (slotNode == null || slotNode.asText().isBlank()) {
                throw new IllegalArgumentException("Missing recommendedSlotId in LLM output: " + clean);
            }

            UUID slotId = UUID.fromString(slotNode.asText().trim());

            // Non-negotiable security guardrail: Check against allowlist
            if (!allowedSlotIds.contains(slotId)) {
                throw new IllegalArgumentException("Hallucination detected: slot " + slotId + " is not in the candidate allowlist " + allowedSlotIds);
            }

            UUID altProviderId = null;
            JsonNode provNode = root.get("alternateProviderId");
            if (provNode == null || provNode.isNull()) provNode = root.get("alternate_provider_id");
            if (provNode == null || provNode.isNull()) provNode = root.get("providerId");
            if (provNode != null && !provNode.isNull() && !provNode.asText().equalsIgnoreCase("null") && !provNode.asText().isBlank()) {
                try {
                    altProviderId = UUID.fromString(provNode.asText().trim());
                } catch (IllegalArgumentException ignored) {}
            }

            BigDecimal confidence = new BigDecimal("0.95");
            JsonNode confNode = root.get("confidenceScore");
            if (confNode == null || confNode.isNull()) confNode = root.get("confidence_score");
            if (confNode == null || confNode.isNull()) confNode = root.get("confidence");
            if (confNode != null && !confNode.isNull()) {
                double confDouble = confNode.asDouble();
                if (confDouble >= 0.0 && confDouble <= 1.0) {
                    confidence = BigDecimal.valueOf(confDouble);
                }
            }

            String script = "";
            if (root.hasNonNull("patientFacingScript")) script = root.get("patientFacingScript").asText();
            else if (root.hasNonNull("patient_facing_script")) script = root.get("patient_facing_script").asText();
            else if (root.hasNonNull("script")) script = root.get("script").asText();

            String rationale = "";
            if (root.hasNonNull("clinicalRationale")) rationale = root.get("clinicalRationale").asText();
            else if (root.hasNonNull("clinical_rationale")) rationale = root.get("clinical_rationale").asText();
            else if (root.hasNonNull("rationale")) rationale = root.get("rationale").asText();

            String fullExplanation = script;
            if (!rationale.isBlank()) {
                fullExplanation = (fullExplanation.isBlank() ? "" : fullExplanation + " ") + "[AI Rationale: " + rationale + "]";
            }

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
