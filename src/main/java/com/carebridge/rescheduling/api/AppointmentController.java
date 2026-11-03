package com.carebridge.rescheduling.api;

import com.carebridge.rescheduling.workflow.dto.AppointmentDetailDTO;
import com.carebridge.rescheduling.workflow.service.AppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<List<AppointmentDetailDTO>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/affected")
    public ResponseEntity<List<AppointmentDetailDTO>> getAffectedAppointments() {
        return ResponseEntity.ok(appointmentService.getAffectedAppointments());
    }

    @PostMapping
    public ResponseEntity<AppointmentDetailDTO> bookAppointment(@RequestBody BookAppointmentRequest req) {
        AppointmentDetailDTO booked = appointmentService.bookAppointment(req.getPatientId(), req.getProviderId(), req.getSlotId());
        return ResponseEntity.status(HttpStatus.CREATED).body(booked);
    }

    public static class BookAppointmentRequest {
        private UUID patientId;
        private UUID providerId;
        private UUID slotId;

        public BookAppointmentRequest() {}
        public BookAppointmentRequest(UUID patientId, UUID providerId, UUID slotId) {
            this.patientId = patientId;
            this.providerId = providerId;
            this.slotId = slotId;
        }

        public UUID getPatientId() { return patientId; }
        public void setPatientId(UUID patientId) { this.patientId = patientId; }
        public UUID getProviderId() { return providerId; }
        public void setProviderId(UUID providerId) { this.providerId = providerId; }
        public UUID getSlotId() { return slotId; }
        public void setSlotId(UUID slotId) { this.slotId = slotId; }
    }
}
