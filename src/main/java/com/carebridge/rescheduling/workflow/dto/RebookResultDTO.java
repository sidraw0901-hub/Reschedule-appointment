package com.carebridge.rescheduling.workflow.dto;

import com.carebridge.rescheduling.domain.model.AppointmentStatus;
import com.carebridge.rescheduling.domain.model.SuggestionStatus;

import java.util.UUID;

public class RebookResultDTO {
    private UUID appointmentId;
    private UUID slotId;
    private AppointmentStatus appointmentStatus;
    private SuggestionStatus suggestionStatus;
    private String message;

    public RebookResultDTO() {}

    public RebookResultDTO(UUID appointmentId, UUID slotId, AppointmentStatus appointmentStatus,
                           SuggestionStatus suggestionStatus, String message) {
        this.appointmentId = appointmentId;
        this.slotId = slotId;
        this.appointmentStatus = appointmentStatus;
        this.suggestionStatus = suggestionStatus;
        this.message = message;
    }

    public UUID getAppointmentId() { return appointmentId; }
    public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }
    public UUID getSlotId() { return slotId; }
    public void setSlotId(UUID slotId) { this.slotId = slotId; }
    public AppointmentStatus getAppointmentStatus() { return appointmentStatus; }
    public void setAppointmentStatus(AppointmentStatus appointmentStatus) { this.appointmentStatus = appointmentStatus; }
    public SuggestionStatus getSuggestionStatus() { return suggestionStatus; }
    public void setSuggestionStatus(SuggestionStatus suggestionStatus) { this.suggestionStatus = suggestionStatus; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
