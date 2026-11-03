package com.carebridge.rescheduling.engine.strategy;

import com.carebridge.rescheduling.domain.model.StrategyType;
import com.carebridge.rescheduling.engine.advisor.LLMGatewayClient;
import com.carebridge.rescheduling.engine.advisor.LLMOutputValidator;
import com.carebridge.rescheduling.engine.model.RecommendationResult;
import com.carebridge.rescheduling.engine.model.RescheduleContext;
import com.carebridge.rescheduling.engine.model.SlotCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Primary
public class AISchedulingStrategy implements SchedulingStrategy {

    private static final Logger log = LoggerFactory.getLogger(AISchedulingStrategy.class);

    private final RuleBasedSchedulingStrategy ruleBasedStrategy;
    private final LLMGatewayClient llmGatewayClient;
    private final LLMOutputValidator outputValidator;
    private final ObjectMapper objectMapper;

    public AISchedulingStrategy(RuleBasedSchedulingStrategy ruleBasedStrategy,
                                LLMGatewayClient llmGatewayClient,
                                LLMOutputValidator outputValidator,
                                ObjectMapper objectMapper) {
        this.ruleBasedStrategy = ruleBasedStrategy;
        this.llmGatewayClient = llmGatewayClient;
        this.outputValidator = outputValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public StrategyType getStrategyType() {
        return StrategyType.AI_POWERED;
    }

    @Override
    public Optional<RecommendationResult> recommend(RescheduleContext context, List<SlotCandidate> eligibleCandidates) {
        if (eligibleCandidates == null || eligibleCandidates.isEmpty()) {
            return Optional.empty();
        }

        Set<UUID> allowedSlotIds = eligibleCandidates.stream()
            .map(SlotCandidate::getSlotId)
            .collect(Collectors.toSet());

        try {
            String systemPrompt = """
                You are a clinical scheduling optimization engine for CareBridge clinics.
                Your task is to select the BEST appointment slot from the given eligible candidate slots for a patient whose appointment was disrupted.
                
                CRITICAL SAFETY RULES:
                1. You MUST select exactly ONE recommendedSlotId from the candidates list.
                2. NEVER hallucinate or invent a slot ID or provider ID.
                3. Return STRICT JSON with keys:
                   - "recommendedSlotId": string (UUID)
                   - "alternateProviderId": string (UUID or null)
                   - "confidenceScore": number between 0.0 and 1.0
                   - "clinicalRationale": string
                   - "patientFacingScript": string (natural empathetic phone script for clinic staff)
                """;

            StringBuilder userPrompt = new StringBuilder();
            userPrompt.append("Disrupted Visit Context:\n")
                .append("- Specialty: ").append(context.getRequiredSpecialty()).append("\n")
                .append("- Priority: ").append(context.getPatientPriority()).append("\n")
                .append("- Original Doctor: ").append(context.getProviderName()).append("\n\n")
                .append("Eligible Candidate Slots (Choose one):\n");

            for (SlotCandidate c : eligibleCandidates) {
                userPrompt.append(String.format("- Slot ID: %s | Provider: %s (Same: %s) | Time: %s | Room: %s | Diff: %d mins\n",
                    c.getSlotId(), c.getProviderName(), c.isOriginalProvider(), c.getStartTime(), c.getRoomName(), c.getMinutesDifferenceFromOriginal()));
            }

            String rawGatewayResponse = llmGatewayClient.callLLMGateway(systemPrompt, userPrompt.toString());

            // Extract content from OpenAI/Gemini format if nested
            String jsonContent = extractContent(rawGatewayResponse);

            LLMOutputValidator.ValidatedLLMOutput validated = outputValidator.validate(jsonContent, allowedSlotIds);

            log.info("AI Advisor successfully recommended slot [{}] with confidence [{}]",
                validated.getSlotId(), validated.getConfidence());

            return Optional.of(RecommendationResult.builder()
                .recommendedSlotId(validated.getSlotId())
                .alternateProviderId(validated.getAlternateProviderId())
                .confidence(validated.getConfidence())
                .explanation(validated.getExplanation())
                .strategyType(StrategyType.AI_POWERED)
                .build());

        } catch (Exception ex) {
            log.warn("AI Advisor unavailable or validation failed ({}). Seamlessly falling back to RuleBasedSchedulingStrategy.", ex.getMessage());
            
            // Execute deterministic rule fallback
            Optional<RecommendationResult> ruleResult = ruleBasedStrategy.recommend(context, eligibleCandidates);
            return ruleResult.map(res -> RecommendationResult.builder()
                .recommendedSlotId(res.getRecommendedSlotId())
                .alternateProviderId(res.getAlternateProviderId())
                .confidence(res.getConfidence())
                .explanation("[AI Fallback Engaged] " + res.getExplanation())
                .strategyType(StrategyType.FALLBACK)
                .build());
        }
    }

    private String extractContent(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            if (root.has("choices") && root.get("choices").isArray() && !root.get("choices").isEmpty()) {
                JsonNode choice = root.get("choices").get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    return choice.get("message").get("content").asText();
                }
            }
        } catch (Exception ignored) {}
        return raw;
    }
}
