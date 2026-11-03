package com.carebridge.rescheduling.workflow.dto;

import com.carebridge.rescheduling.domain.model.ProviderStatus;

import java.util.UUID;

public class ProviderBandwidthDTO {
    private UUID providerId;
    private String providerName;
    private String specialty;
    private ProviderStatus status;
    private int totalSlots;
    private int bookedSlots;
    private int availableBandwidth;
    private int utilizationPercentage;
    private String roomName;

    public ProviderBandwidthDTO() {}

    public ProviderBandwidthDTO(UUID providerId, String providerName, String specialty, ProviderStatus status,
                                int totalSlots, int bookedSlots, int availableBandwidth,
                                int utilizationPercentage, String roomName) {
        this.providerId = providerId;
        this.providerName = providerName;
        this.specialty = specialty;
        this.status = status;
        this.totalSlots = totalSlots;
        this.bookedSlots = bookedSlots;
        this.availableBandwidth = availableBandwidth;
        this.utilizationPercentage = utilizationPercentage;
        this.roomName = roomName;
    }

    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public ProviderStatus getStatus() { return status; }
    public void setStatus(ProviderStatus status) { this.status = status; }
    public int getTotalSlots() { return totalSlots; }
    public void setTotalSlots(int totalSlots) { this.totalSlots = totalSlots; }
    public int getBookedSlots() { return bookedSlots; }
    public void setBookedSlots(int bookedSlots) { this.bookedSlots = bookedSlots; }
    public int getAvailableBandwidth() { return availableBandwidth; }
    public void setAvailableBandwidth(int availableBandwidth) { this.availableBandwidth = availableBandwidth; }
    public int getUtilizationPercentage() { return utilizationPercentage; }
    public void setUtilizationPercentage(int utilizationPercentage) { this.utilizationPercentage = utilizationPercentage; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
}
