package com.carebridge.rescheduling.api;

import com.carebridge.rescheduling.domain.model.*;
import com.carebridge.rescheduling.domain.repository.*;
import com.carebridge.rescheduling.outbox.model.OutboxStatus;
import com.carebridge.rescheduling.outbox.repository.OutboxEventRepository;
import com.carebridge.rescheduling.workflow.dto.RebookResultDTO;
import com.carebridge.rescheduling.workflow.dto.RejectRequest;
import com.carebridge.rescheduling.workflow.dto.SuggestionDetailDTO;
import com.carebridge.rescheduling.workflow.service.SuggestionApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/suggestions")
@CrossOrigin(origins = "*")
public class SuggestionController {

    private final RescheduleSuggestionRepository suggestionRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ProviderRepository providerRepository;
    private final SlotRepository slotRepository;
    private final RoomRepository roomRepository;
    private final SuggestionApprovalService approvalService;
    private final OutboxEventRepository outboxEventRepository;

    public SuggestionController(RescheduleSuggestionRepository suggestionRepository,
                                AppointmentRepository appointmentRepository,
                                PatientRepository patientRepository,
                                ProviderRepository providerRepository,
                                SlotRepository slotRepository,
                                RoomRepository roomRepository,
                                SuggestionApprovalService approvalService,
                                OutboxEventRepository outboxEventRepository) {
        this.suggestionRepository = suggestionRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.providerRepository = providerRepository;
        this.slotRepository = slotRepository;
        this.roomRepository = roomRepository;
        this.approvalService = approvalService;
        this.outboxEventRepository = outboxEventRepository;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<SuggestionDetailDTO>> getPendingSuggestions() {
        List<RescheduleSuggestion> list = suggestionRepository.findByStatus(SuggestionStatus.PENDING);
        return ResponseEntity.ok(list.stream().map(this::toDetailDTO).collect(Collectors.toList()));
    }

    @GetMapping
    public ResponseEntity<List<SuggestionDetailDTO>> getAllSuggestions() {
        List<RescheduleSuggestion> list = suggestionRepository.findAll();
        return ResponseEntity.ok(list.stream().map(this::toDetailDTO).collect(Collectors.toList()));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<RebookResultDTO> acceptSuggestion(@PathVariable UUID id) {
        RebookResultDTO result = approvalService.acceptSuggestion(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectSuggestion(
            @PathVariable UUID id,
            @RequestBody(required = false) RejectRequest request) {

        String reason = request != null ? request.getReason() : "Declined by patient or coordinator";
        approvalService.rejectSuggestion(id, reason);
        return ResponseEntity.ok(Map.of(
            "message", "Suggestion rejected",
            "suggestionId", id,
            "status", "REJECTED"
        ));
    }

    @GetMapping("/system/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        long totalAppointments = appointmentRepository.count();
        long affectedAppointments = appointmentRepository.findByStatus(AppointmentStatus.RESCHEDULE_REQUIRED).size();
        long pendingSuggestions = suggestionRepository.findByStatus(SuggestionStatus.PENDING).size();
        long rescheduledAppointments = appointmentRepository.findByStatus(AppointmentStatus.RESCHEDULED).size();
        long totalOutboxEvents = outboxEventRepository.count();

        return ResponseEntity.ok(Map.of(
            "totalAppointments", totalAppointments,
            "affectedAppointments", affectedAppointments,
            "pendingSuggestions", pendingSuggestions,
            "rescheduledAppointments", rescheduledAppointments,
            "totalOutboxEvents", totalOutboxEvents,
            "workerStatus", "ACTIVE"
        ));
    }

    private SuggestionDetailDTO toDetailDTO(RescheduleSuggestion s) {
        Appointment appt = appointmentRepository.findById(s.getAppointmentId()).orElse(null);
        Patient patient = (appt != null) ? patientRepository.findById(appt.getPatientId()).orElse(null) : null;
        Provider origProvider = (appt != null) ? providerRepository.findById(appt.getProviderId()).orElse(null) : null;
        Slot origSlot = (appt != null) ? slotRepository.findById(appt.getSlotId()).orElse(null) : null;

        Slot recSlot = slotRepository.findById(s.getRecommendedSlotId()).orElse(null);
        Provider recProvider = (recSlot != null) ? providerRepository.findById(recSlot.getProviderId()).orElse(null) : null;
        Room room = (recSlot != null) ? roomRepository.findById(recSlot.getRoomId()).orElse(null) : null;

        return SuggestionDetailDTO.builder()
            .suggestionId(s.getId())
            .appointmentId(s.getAppointmentId())
            .patientName(patient != null ? patient.getFullName() : "Patient")
            .patientPhone(patient != null ? patient.getPhone() : "")
            .priorityLevel(patient != null ? patient.getPriorityLevel().name() : "NORMAL")
            .originalProviderName(origProvider != null ? origProvider.getName() : "Doctor")
            .originalSlotTime(origSlot != null ? origSlot.getStartTime() : null)
            .recommendedSlotId(s.getRecommendedSlotId())
            .recommendedProviderName(recProvider != null ? recProvider.getName() : "Doctor")
            .recommendedSlotTime(recSlot != null ? recSlot.getStartTime() : null)
            .roomName(room != null ? room.getName() : "Exam Room")
            .confidence(s.getConfidence())
            .triggerType(s.getTriggerType())
            .strategyType(s.getStrategyType())
            .status(s.getStatus())
            .explanation(s.getExplanation())
            .build();
    }
}
