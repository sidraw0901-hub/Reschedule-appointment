package com.carebridge.rescheduling.engine.model;

import com.carebridge.rescheduling.domain.model.TriggerType;
import java.time.Instant;
import java.util.UUID;

public class RescheduleContext {
    private UUID appointmentId;
    private UUID patientId;
    private String patientName;
    private String patientPhone;
    private String patientPriority;
    private UUID currentProviderId;
    private String providerName;
    private String requiredSpecialty;
    private Instant originalStartTime;
    private Instant originalEndTime;
    private TriggerType triggerType;

    public RescheduleContext() {}

    public RescheduleContext(UUID appointmentId, UUID patientId, String patientName, String patientPhone,
                             String patientPriority, UUID currentProviderId, String providerName,
                             String requiredSpecialty, Instant originalStartTime, Instant originalEndTime,
                             TriggerType triggerType) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.patientPriority = patientPriority;
        this.currentProviderId = currentProviderId;
        this.providerName = providerName;
        this.requiredSpecialty = requiredSpecialty;
        this.originalStartTime = originalStartTime;
        this.originalEndTime = originalEndTime;
        this.triggerType = triggerType;
    }

    public UUID getAppointmentId() { return appointmentId; }
    public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }
    public String getPatientPriority() { return patientPriority; }
    public void setPatientPriority(String patientPriority) { this.patientPriority = patientPriority; }
    public UUID getCurrentProviderId() { return currentProviderId; }
    public void setCurrentProviderId(UUID currentProviderId) { this.currentProviderId = currentProviderId; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getRequiredSpecialty() { return requiredSpecialty; }
    public void setRequiredSpecialty(String requiredSpecialty) { this.requiredSpecialty = requiredSpecialty; }
    public Instant getOriginalStartTime() { return originalStartTime; }
    public void setOriginalStartTime(Instant originalStartTime) { this.originalStartTime = originalStartTime; }
    public Instant getOriginalEndTime() { return originalEndTime; }
    public void setOriginalEndTime(Instant originalEndTime) { this.originalEndTime = originalEndTime; }
    public TriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(TriggerType triggerType) { this.triggerType = triggerType; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID appointmentId;
        private UUID patientId;
        private String patientName;
        private String patientPhone;
        private String patientPriority;
        private UUID currentProviderId;
        private String providerName;
        private String requiredSpecialty;
        private Instant originalStartTime;
        private Instant originalEndTime;
        private TriggerType triggerType;

        public Builder appointmentId(UUID appointmentId) { this.appointmentId = appointmentId; return this; }
        public Builder patientId(UUID patientId) { this.patientId = patientId; return this; }
        public Builder patientName(String patientName) { this.patientName = patientName; return this; }
        public Builder patientPhone(String patientPhone) { this.patientPhone = patientPhone; return this; }
        public Builder patientPriority(String patientPriority) { this.patientPriority = patientPriority; return this; }
        public Builder currentProviderId(UUID currentProviderId) { this.currentProviderId = currentProviderId; return this; }
        public Builder providerName(String providerName) { this.providerName = providerName; return this; }
        public Builder requiredSpecialty(String requiredSpecialty) { this.requiredSpecialty = requiredSpecialty; return this; }
        public Builder originalStartTime(Instant originalStartTime) { this.originalStartTime = originalStartTime; return this; }
        public Builder originalEndTime(Instant originalEndTime) { this.originalEndTime = originalEndTime; return this; }
        public Builder triggerType(TriggerType triggerType) { this.triggerType = triggerType; return this; }

        public RescheduleContext build() {
            return new RescheduleContext(appointmentId, patientId, patientName, patientPhone, patientPriority,
                    currentProviderId, providerName, requiredSpecialty, originalStartTime, originalEndTime, triggerType);
        }
    }
}
