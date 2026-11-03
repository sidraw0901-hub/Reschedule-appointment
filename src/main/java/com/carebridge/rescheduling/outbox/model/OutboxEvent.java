package com.carebridge.rescheduling.outbox.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(nullable = false)
    private Instant scheduledAt;

    private Instant processedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public OutboxEvent() {}

    public OutboxEvent(UUID id, String aggregateType, UUID aggregateId, String eventType, String payload,
                       OutboxStatus status, Integer retryCount, Instant scheduledAt, Instant processedAt, String errorMessage) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status != null ? status : OutboxStatus.PENDING;
        this.retryCount = retryCount != null ? retryCount : 0;
        this.scheduledAt = scheduledAt != null ? scheduledAt : Instant.now();
        this.processedAt = processedAt;
        this.errorMessage = errorMessage;
    }

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = OutboxStatus.PENDING;
        }
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
        if (this.scheduledAt == null) {
            this.scheduledAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public void setAggregateId(UUID aggregateId) { this.aggregateId = aggregateId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public OutboxStatus getStatus() { return status; }
    public void setStatus(OutboxStatus status) { this.status = status; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String aggregateType;
        private UUID aggregateId;
        private String eventType;
        private String payload;
        private OutboxStatus status;
        private Integer retryCount;
        private Instant scheduledAt;
        private Instant processedAt;
        private String errorMessage;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder aggregateType(String aggregateType) { this.aggregateType = aggregateType; return this; }
        public Builder aggregateId(UUID aggregateId) { this.aggregateId = aggregateId; return this; }
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder payload(String payload) { this.payload = payload; return this; }
        public Builder status(OutboxStatus status) { this.status = status; return this; }
        public Builder retryCount(Integer retryCount) { this.retryCount = retryCount; return this; }
        public Builder scheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; return this; }
        public Builder processedAt(Instant processedAt) { this.processedAt = processedAt; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }

        public OutboxEvent build() {
            return new OutboxEvent(id, aggregateType, aggregateId, eventType, payload, status, retryCount, scheduledAt, processedAt, errorMessage);
        }
    }
}
