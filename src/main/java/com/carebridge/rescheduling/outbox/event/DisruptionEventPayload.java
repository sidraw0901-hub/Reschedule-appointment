package com.carebridge.rescheduling.outbox.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DisruptionEventPayload {
    private String eventType;
    private UUID providerId;
    private UUID slotId;
    private String reason;
    private List<UUID> affectedAppointmentIds;
    private Instant occurredAt;

    public DisruptionEventPayload() {}

    public DisruptionEventPayload(String eventType, UUID providerId, UUID slotId, String reason,
                                 List<UUID> affectedAppointmentIds, Instant occurredAt) {
        this.eventType = eventType;
        this.providerId = providerId;
        this.slotId = slotId;
        this.reason = reason;
        this.affectedAppointmentIds = affectedAppointmentIds;
        this.occurredAt = occurredAt;
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public UUID getSlotId() { return slotId; }
    public void setSlotId(UUID slotId) { this.slotId = slotId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public List<UUID> getAffectedAppointmentIds() { return affectedAppointmentIds; }
    public void setAffectedAppointmentIds(List<UUID> affectedAppointmentIds) { this.affectedAppointmentIds = affectedAppointmentIds; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String eventType;
        private UUID providerId;
        private UUID slotId;
        private String reason;
        private List<UUID> affectedAppointmentIds;
        private Instant occurredAt;

        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder providerId(UUID providerId) { this.providerId = providerId; return this; }
        public Builder slotId(UUID slotId) { this.slotId = slotId; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder affectedAppointmentIds(List<UUID> affectedAppointmentIds) { this.affectedAppointmentIds = affectedAppointmentIds; return this; }
        public Builder occurredAt(Instant occurredAt) { this.occurredAt = occurredAt; return this; }

        public DisruptionEventPayload build() {
            return new DisruptionEventPayload(eventType, providerId, slotId, reason, affectedAppointmentIds, occurredAt);
        }
    }
}
