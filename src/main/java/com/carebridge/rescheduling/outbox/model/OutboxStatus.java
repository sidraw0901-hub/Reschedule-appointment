package com.carebridge.rescheduling.outbox.model;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    DEAD_LETTER
}
