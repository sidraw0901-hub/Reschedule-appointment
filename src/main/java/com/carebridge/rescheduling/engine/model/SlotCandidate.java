package com.carebridge.rescheduling.engine.model;

import java.time.Instant;
import java.util.UUID;

public class SlotCandidate {
    private UUID slotId;
    private UUID providerId;
    private String providerName;
    private String specialty;
    private boolean originalProvider;
    private UUID roomId;
    private String roomName;
    private Instant startTime;
    private Instant endTime;
    private long minutesDifferenceFromOriginal;

    public SlotCandidate() {}

    public SlotCandidate(UUID slotId, UUID providerId, String providerName, String specialty,
                         boolean originalProvider, UUID roomId, String roomName,
                         Instant startTime, Instant endTime, long minutesDifferenceFromOriginal) {
        this.slotId = slotId;
        this.providerId = providerId;
        this.providerName = providerName;
        this.specialty = specialty;
        this.originalProvider = originalProvider;
        this.roomId = roomId;
        this.roomName = roomName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minutesDifferenceFromOriginal = minutesDifferenceFromOriginal;
    }

    public UUID getSlotId() { return slotId; }
    public void setSlotId(UUID slotId) { this.slotId = slotId; }
    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public boolean isOriginalProvider() { return originalProvider; }
    public void setOriginalProvider(boolean originalProvider) { this.originalProvider = originalProvider; }
    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public long getMinutesDifferenceFromOriginal() { return minutesDifferenceFromOriginal; }
    public void setMinutesDifferenceFromOriginal(long minutesDifferenceFromOriginal) { this.minutesDifferenceFromOriginal = minutesDifferenceFromOriginal; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID slotId;
        private UUID providerId;
        private String providerName;
        private String specialty;
        private boolean originalProvider;
        private UUID roomId;
        private String roomName;
        private Instant startTime;
        private Instant endTime;
        private long minutesDifferenceFromOriginal;

        public Builder slotId(UUID slotId) { this.slotId = slotId; return this; }
        public Builder providerId(UUID providerId) { this.providerId = providerId; return this; }
        public Builder providerName(String providerName) { this.providerName = providerName; return this; }
        public Builder specialty(String specialty) { this.specialty = specialty; return this; }
        public Builder originalProvider(boolean originalProvider) { this.originalProvider = originalProvider; return this; }
        public Builder roomId(UUID roomId) { this.roomId = roomId; return this; }
        public Builder roomName(String roomName) { this.roomName = roomName; return this; }
        public Builder startTime(Instant startTime) { this.startTime = startTime; return this; }
        public Builder endTime(Instant endTime) { this.endTime = endTime; return this; }
        public Builder minutesDifferenceFromOriginal(long minutesDifferenceFromOriginal) { this.minutesDifferenceFromOriginal = minutesDifferenceFromOriginal; return this; }

        public SlotCandidate build() {
            return new SlotCandidate(slotId, providerId, providerName, specialty, originalProvider, roomId, roomName, startTime, endTime, minutesDifferenceFromOriginal);
        }
    }
}
