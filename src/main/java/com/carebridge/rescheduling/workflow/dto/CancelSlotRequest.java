package com.carebridge.rescheduling.workflow.dto;

public class CancelSlotRequest {
    private String reason;

    public CancelSlotRequest() {}
    public CancelSlotRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String reason;
        public Builder reason(String reason) { this.reason = reason; return this; }
        public CancelSlotRequest build() { return new CancelSlotRequest(reason); }
    }
}
