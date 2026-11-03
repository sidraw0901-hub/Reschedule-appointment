package com.carebridge.rescheduling.workflow.dto;

import java.time.Instant;

public class DisruptionRequest {
    private String reason;
    private Instant effectiveUntil;

    public DisruptionRequest() {}
    public DisruptionRequest(String reason) {
        this.reason = reason;
    }
    public DisruptionRequest(String reason, Instant effectiveUntil) {
        this.reason = reason;
        this.effectiveUntil = effectiveUntil;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getEffectiveUntil() { return effectiveUntil; }
    public void setEffectiveUntil(Instant effectiveUntil) { this.effectiveUntil = effectiveUntil; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String reason;
        private Instant effectiveUntil;
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder effectiveUntil(Instant effectiveUntil) { this.effectiveUntil = effectiveUntil; return this; }
        public DisruptionRequest build() { return new DisruptionRequest(reason, effectiveUntil); }
    }
}
