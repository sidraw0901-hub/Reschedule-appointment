package com.carebridge.rescheduling.workflow.dto;

import com.carebridge.rescheduling.domain.model.StrategyType;
import com.carebridge.rescheduling.domain.model.SuggestionStatus;
import com.carebridge.rescheduling.domain.model.TriggerType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class SuggestionDetailDTO {
    private UUID suggestionId;
    private UUID appointmentId;
    private String patientName;
    private String patientPhone;
    private String priorityLevel;
    private String originalProviderName;
    private Instant originalSlotTime;
    private UUID recommendedSlotId;
    private String recommendedProviderName;
    private Instant recommendedSlotTime;
    private String roomName;
    private BigDecimal confidence;
    private TriggerType triggerType;
    private StrategyType strategyType;
    private SuggestionStatus status;
    private String explanation;

    public SuggestionDetailDTO() {}

    public SuggestionDetailDTO(UUID suggestionId, UUID appointmentId, String patientName, String patientPhone,
                               String priorityLevel, String originalProviderName, Instant originalSlotTime,
                               UUID recommendedSlotId, String recommendedProviderName, Instant recommendedSlotTime,
                               String roomName, BigDecimal confidence, TriggerType triggerType,
                               StrategyType strategyType, SuggestionStatus status, String explanation) {
        this.suggestionId = suggestionId;
        this.appointmentId = appointmentId;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.priorityLevel = priorityLevel;
        this.originalProviderName = originalProviderName;
        this.originalSlotTime = originalSlotTime;
        this.recommendedSlotId = recommendedSlotId;
        this.recommendedProviderName = recommendedProviderName;
        this.recommendedSlotTime = recommendedSlotTime;
        this.roomName = roomName;
        this.confidence = confidence;
        this.triggerType = triggerType;
        this.strategyType = strategyType;
        this.status = status;
        this.explanation = explanation;
    }

    public UUID getSuggestionId() { return suggestionId; }
    public void setSuggestionId(UUID suggestionId) { this.suggestionId = suggestionId; }
    public UUID getAppointmentId() { return appointmentId; }
    public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }
    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }
    public String getOriginalProviderName() { return originalProviderName; }
    public void setOriginalProviderName(String originalProviderName) { this.originalProviderName = originalProviderName; }
    public Instant getOriginalSlotTime() { return originalSlotTime; }
    public void setOriginalSlotTime(Instant originalSlotTime) { this.originalSlotTime = originalSlotTime; }
    public UUID getRecommendedSlotId() { return recommendedSlotId; }
    public void setRecommendedSlotId(UUID recommendedSlotId) { this.recommendedSlotId = recommendedSlotId; }
    public String getRecommendedProviderName() { return recommendedProviderName; }
    public void setRecommendedProviderName(String recommendedProviderName) { this.recommendedProviderName = recommendedProviderName; }
    public Instant getRecommendedSlotTime() { return recommendedSlotTime; }
    public void setRecommendedSlotTime(Instant recommendedSlotTime) { this.recommendedSlotTime = recommendedSlotTime; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }
    public StrategyType getStrategyType() { return strategyType; }
    public void setStrategyType(StrategyType strategyType) { this.strategyType = strategyType; }
    public SuggestionStatus getStatus() { return status; }
    public void setStatus(SuggestionStatus status) { this.status = status; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID suggestionId;
        private UUID appointmentId;
        private String patientName;
        private String patientPhone;
        private String priorityLevel;
        private String originalProviderName;
        private Instant originalSlotTime;
        private UUID recommendedSlotId;
        private String recommendedProviderName;
        private Instant recommendedSlotTime;
        private String roomName;
        private BigDecimal confidence;
        private TriggerType triggerType;
        private StrategyType strategyType;
        private SuggestionStatus status;
        private String explanation;

        public Builder suggestionId(UUID suggestionId) { this.suggestionId = suggestionId; return this; }
        public Builder appointmentId(UUID appointmentId) { this.appointmentId = appointmentId; return this; }
        public Builder patientName(String patientName) { this.patientName = patientName; return this; }
        public Builder patientPhone(String patientPhone) { this.patientPhone = patientPhone; return this; }
        public Builder priorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; return this; }
        public Builder originalProviderName(String originalProviderName) { this.originalProviderName = originalProviderName; return this; }
        public Builder originalSlotTime(Instant originalSlotTime) { this.originalSlotTime = originalSlotTime; return this; }
        public Builder recommendedSlotId(UUID recommendedSlotId) { this.recommendedSlotId = recommendedSlotId; return this; }
        public Builder recommendedProviderName(String recommendedProviderName) { this.recommendedProviderName = recommendedProviderName; return this; }
        public Builder recommendedSlotTime(Instant recommendedSlotTime) { this.recommendedSlotTime = recommendedSlotTime; return this; }
        public Builder roomName(String roomName) { this.roomName = roomName; return this; }
        public Builder confidence(BigDecimal confidence) { this.confidence = confidence; return this; }
        public Builder triggerType(TriggerType triggerType) { this.triggerType = triggerType; return this; }
        public Builder strategyType(StrategyType strategyType) { this.strategyType = strategyType; return this; }
        public Builder status(SuggestionStatus status) { this.status = status; return this; }
        public Builder explanation(String explanation) { this.explanation = explanation; return this; }

        public SuggestionDetailDTO build() {
            return new SuggestionDetailDTO(suggestionId, appointmentId, patientName, patientPhone, priorityLevel,
                    originalProviderName, originalSlotTime, recommendedSlotId, recommendedProviderName,
                    recommendedSlotTime, roomName, confidence, triggerType, strategyType, status, explanation);
        }
    }
}
