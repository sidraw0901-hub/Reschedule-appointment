package com.carebridge.rescheduling.engine.model;

import com.carebridge.rescheduling.domain.model.StrategyType;
import java.math.BigDecimal;
import java.util.UUID;

public class RecommendationResult {
    private UUID recommendedSlotId;
    private UUID alternateProviderId;
    private BigDecimal confidence;
    private String explanation;
    private StrategyType strategyType;

    public RecommendationResult() {}

    public RecommendationResult(UUID recommendedSlotId, UUID alternateProviderId, BigDecimal confidence,
                                String explanation, StrategyType strategyType) {
        this.recommendedSlotId = recommendedSlotId;
        this.alternateProviderId = alternateProviderId;
        this.confidence = confidence;
        this.explanation = explanation;
        this.strategyType = strategyType;
    }

    public UUID getRecommendedSlotId() { return recommendedSlotId; }
    public void setRecommendedSlotId(UUID recommendedSlotId) { this.recommendedSlotId = recommendedSlotId; }
    public UUID getAlternateProviderId() { return alternateProviderId; }
    public void setAlternateProviderId(UUID alternateProviderId) { this.alternateProviderId = alternateProviderId; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public StrategyType getStrategyType() { return strategyType; }
    public void setStrategyType(StrategyType strategyType) { this.strategyType = strategyType; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID recommendedSlotId;
        private UUID alternateProviderId;
        private BigDecimal confidence;
        private String explanation;
        private StrategyType strategyType;

        public Builder recommendedSlotId(UUID recommendedSlotId) { this.recommendedSlotId = recommendedSlotId; return this; }
        public Builder alternateProviderId(UUID alternateProviderId) { this.alternateProviderId = alternateProviderId; return this; }
        public Builder confidence(BigDecimal confidence) { this.confidence = confidence; return this; }
        public Builder explanation(String explanation) { this.explanation = explanation; return this; }
        public Builder strategyType(StrategyType strategyType) { this.strategyType = strategyType; return this; }

        public RecommendationResult build() {
            return new RecommendationResult(recommendedSlotId, alternateProviderId, confidence, explanation, strategyType);
        }
    }
}
