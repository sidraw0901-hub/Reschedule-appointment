package com.carebridge.rescheduling.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reschedule_suggestions")
public class RescheduleSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID appointmentId;

    @Column(nullable = false)
    private UUID recommendedSlotId;

    private UUID alternateProviderId;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StrategyType strategyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionStatus status;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public RescheduleSuggestion() {}

    public RescheduleSuggestion(UUID id, UUID appointmentId, UUID recommendedSlotId, UUID alternateProviderId,
                                BigDecimal confidence, String explanation, TriggerType triggerType,
                                StrategyType strategyType, SuggestionStatus status) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.recommendedSlotId = recommendedSlotId;
        this.alternateProviderId = alternateProviderId;
        this.confidence = confidence;
        this.explanation = explanation;
        this.triggerType = triggerType;
        this.strategyType = strategyType;
        this.status = status != null ? status : SuggestionStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = SuggestionStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAppointmentId() { return appointmentId; }
    public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }
    public UUID getRecommendedSlotId() { return recommendedSlotId; }
    public void setRecommendedSlotId(UUID recommendedSlotId) { this.recommendedSlotId = recommendedSlotId; }
    public UUID getAlternateProviderId() { return alternateProviderId; }
    public void setAlternateProviderId(UUID alternateProviderId) { this.alternateProviderId = alternateProviderId; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }
    public StrategyType getStrategyType() { return strategyType; }
    public void setStrategyType(StrategyType strategyType) { this.strategyType = strategyType; }
    public SuggestionStatus getStatus() { return status; }
    public void setStatus(SuggestionStatus status) { this.status = status; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID appointmentId;
        private UUID recommendedSlotId;
        private UUID alternateProviderId;
        private BigDecimal confidence;
        private String explanation;
        private TriggerType triggerType;
        private StrategyType strategyType;
        private SuggestionStatus status;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder appointmentId(UUID appointmentId) { this.appointmentId = appointmentId; return this; }
        public Builder recommendedSlotId(UUID recommendedSlotId) { this.recommendedSlotId = recommendedSlotId; return this; }
        public Builder alternateProviderId(UUID alternateProviderId) { this.alternateProviderId = alternateProviderId; return this; }
        public Builder confidence(BigDecimal confidence) { this.confidence = confidence; return this; }
        public Builder explanation(String explanation) { this.explanation = explanation; return this; }
        public Builder triggerType(TriggerType triggerType) { this.triggerType = triggerType; return this; }
        public Builder strategyType(StrategyType strategyType) { this.strategyType = strategyType; return this; }
        public Builder status(SuggestionStatus status) { this.status = status; return this; }

        public RescheduleSuggestion build() {
            return new RescheduleSuggestion(id, appointmentId, recommendedSlotId, alternateProviderId,
                    confidence, explanation, triggerType, strategyType, status);
        }
    }
}
