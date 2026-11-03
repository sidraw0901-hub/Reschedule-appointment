package com.carebridge.rescheduling.workflow.dto;

import com.carebridge.rescheduling.domain.model.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;

public class AppointmentDetailDTO {
    private UUID appointmentId;
    private UUID patientId;
    private String patientName;
    private String patientPhone;
    private String priorityLevel;
    private UUID providerId;
    private String providerName;
    private String specialty;
    private UUID slotId;
    private Instant startTime;
    private Instant endTime;
    private String roomName;
    private AppointmentStatus status;

    public AppointmentDetailDTO() {}

    public AppointmentDetailDTO(UUID appointmentId, UUID patientId, String patientName, String patientPhone,
                                String priorityLevel, UUID providerId, String providerName, String specialty,
                                UUID slotId, Instant startTime, Instant endTime, String roomName, AppointmentStatus status) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.priorityLevel = priorityLevel;
        this.providerId = providerId;
        this.providerName = providerName;
        this.specialty = specialty;
        this.slotId = slotId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.roomName = roomName;
        this.status = status;
    }

    public UUID getAppointmentId() { return appointmentId; }
    public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }
    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }
    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }
    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public UUID getSlotId() { return slotId; }
    public void setSlotId(UUID slotId) { this.slotId = slotId; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID appointmentId;
        private UUID patientId;
        private String patientName;
        private String patientPhone;
        private String priorityLevel;
        private UUID providerId;
        private String providerName;
        private String specialty;
        private UUID slotId;
        private Instant startTime;
        private Instant endTime;
        private String roomName;
        private AppointmentStatus status;

        public Builder appointmentId(UUID appointmentId) { this.appointmentId = appointmentId; return this; }
        public Builder patientId(UUID patientId) { this.patientId = patientId; return this; }
        public Builder patientName(String patientName) { this.patientName = patientName; return this; }
        public Builder patientPhone(String patientPhone) { this.patientPhone = patientPhone; return this; }
        public Builder priorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; return this; }
        public Builder providerId(UUID providerId) { this.providerId = providerId; return this; }
        public Builder providerName(String providerName) { this.providerName = providerName; return this; }
        public Builder specialty(String specialty) { this.specialty = specialty; return this; }
        public Builder slotId(UUID slotId) { this.slotId = slotId; return this; }
        public Builder startTime(Instant startTime) { this.startTime = startTime; return this; }
        public Builder endTime(Instant endTime) { this.endTime = endTime; return this; }
        public Builder roomName(String roomName) { this.roomName = roomName; return this; }
        public Builder status(AppointmentStatus status) { this.status = status; return this; }

        public AppointmentDetailDTO build() {
            return new AppointmentDetailDTO(appointmentId, patientId, patientName, patientPhone,
                    priorityLevel, providerId, providerName, specialty, slotId, startTime, endTime, roomName, status);
        }
    }
}
