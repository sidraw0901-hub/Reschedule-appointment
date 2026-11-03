package com.carebridge.rescheduling.workflow.service;

import com.carebridge.rescheduling.common.exception.ConflictException;
import com.carebridge.rescheduling.domain.model.*;
import com.carebridge.rescheduling.domain.repository.AppointmentRepository;
import com.carebridge.rescheduling.domain.repository.PatientRepository;
import com.carebridge.rescheduling.domain.repository.ProviderRepository;
import com.carebridge.rescheduling.domain.repository.RescheduleSuggestionRepository;
import com.carebridge.rescheduling.domain.repository.RoomRepository;
import com.carebridge.rescheduling.domain.repository.SlotRepository;
import com.carebridge.rescheduling.engine.service.RescheduleOrchestrator;
import com.carebridge.rescheduling.workflow.dto.DisruptionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CascadingDisruptionTest {

    @Autowired
    private DisruptionService disruptionService;

    @Autowired
    private SuggestionApprovalService approvalService;

    @Autowired
    private RescheduleOrchestrator rescheduleOrchestrator;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private RescheduleSuggestionRepository suggestionRepository;

    @Test
    @DisplayName("Cascading Disruption: Doctor A -> Rebooked to Doctor B -> Doctor B also goes OFFLINE -> Rebooked to Doctor C")
    void testCascadingDisruptionDoctorAToDoctorBToDoctorC() {
        // 1. Setup 3 Oncology doctors
        Provider docA = providerRepository.save(new Provider(null, "Dr. Alice Adams", "Oncology", ProviderStatus.AVAILABLE));
        Provider docB = providerRepository.save(new Provider(null, "Dr. Bob Baker", "Oncology", ProviderStatus.AVAILABLE));
        Provider docC = providerRepository.save(new Provider(null, "Dr. Clara Cole", "Oncology", ProviderStatus.AVAILABLE));

        Room room = roomRepository.save(new Room(null, "Oncology Suite 7", "Infusion, Vitals", RoomStatus.AVAILABLE));
        Patient patient = patientRepository.save(new Patient(null, "Samantha", "Ray", "(555) 777-8888", PriorityLevel.HIGH));

        Instant baseTime = Instant.now().plus(2, ChronoUnit.HOURS);

        // Doc A has booked slot & appointment
        Slot slotA = slotRepository.save(new Slot(null, docA.getId(), room.getId(), baseTime, baseTime.plus(30, ChronoUnit.MINUTES), SlotStatus.BOOKED));
        Appointment appt = appointmentRepository.save(new Appointment(null, patient.getId(), docA.getId(), slotA.getId(), AppointmentStatus.BOOKED));

        // Doc B has available slot
        Slot slotB = slotRepository.save(new Slot(null, docB.getId(), room.getId(), baseTime.plus(30, ChronoUnit.MINUTES), baseTime.plus(60, ChronoUnit.MINUTES), SlotStatus.AVAILABLE));

        // Doc C has available slot
        Slot slotC = slotRepository.save(new Slot(null, docC.getId(), room.getId(), baseTime.plus(60, ChronoUnit.MINUTES), baseTime.plus(90, ChronoUnit.MINUTES), SlotStatus.AVAILABLE));

        // ==========================================
        // STAGE 1: Doctor A goes OFFLINE
        // ==========================================
        disruptionService.markProviderOffline(docA.getId(), new DisruptionRequest("Doctor A family emergency"));
        
        Appointment apptAfterDisruption1 = appointmentRepository.findById(appt.getId()).orElseThrow();
        assertEquals(AppointmentStatus.RESCHEDULE_REQUIRED, apptAfterDisruption1.getStatus());

        // Process recommendation for Doctor A's patient
        Optional<RescheduleSuggestion> suggestion1 = rescheduleOrchestrator.processSingleAppointment(appt.getId(), TriggerType.AUTO_PROVIDER_OFFLINE);
        assertTrue(suggestion1.isPresent());
        assertEquals(slotB.getId(), suggestion1.get().getRecommendedSlotId());

        // Coordinator accepts recommendation to Doctor B
        approvalService.acceptSuggestion(suggestion1.get().getId());

        Appointment apptRebookedB = appointmentRepository.findById(appt.getId()).orElseThrow();
        assertEquals(AppointmentStatus.RESCHEDULED, apptRebookedB.getStatus());
        assertEquals(docB.getId(), apptRebookedB.getProviderId(), "Appointment doctor must be updated to Doctor B");
        assertEquals(slotB.getId(), apptRebookedB.getSlotId(), "Appointment slot must be updated to Slot B");

        // ==========================================
        // STAGE 2: Doctor B ALSO goes OFFLINE! (The Secondary Disruption Edge Case)
        // ==========================================
        disruptionService.markProviderOffline(docB.getId(), new DisruptionRequest("Doctor B unexpected flu"));

        // Doctor B's provider status is OFFLINE
        assertEquals(ProviderStatus.OFFLINE, providerRepository.findById(docB.getId()).orElseThrow().getStatus());

        // The appointment that was rescheduled to Doctor B must now enter RESCHEDULE_REQUIRED again!
        Appointment apptAfterDisruption2 = appointmentRepository.findById(appt.getId()).orElseThrow();
        assertEquals(AppointmentStatus.RESCHEDULE_REQUIRED, apptAfterDisruption2.getStatus(),
            "Secondary disruption must catch RESCHEDULED appointments and set status back to RESCHEDULE_REQUIRED");

        // Engine processes recommendation: Doc A is offline, Doc B is offline -> Doc C must be chosen!
        Optional<RescheduleSuggestion> suggestion2 = rescheduleOrchestrator.processSingleAppointment(appt.getId(), TriggerType.AUTO_PROVIDER_OFFLINE);
        assertTrue(suggestion2.isPresent(), "Engine should find available slot with Doctor C");
        assertEquals(slotC.getId(), suggestion2.get().getRecommendedSlotId(), "Must recommend Doctor C's slot");
        assertEquals(docC.getId(), suggestion2.get().getAlternateProviderId(), "Alternate provider must be Doctor C");

        // Coordinator accepts recommendation to Doctor C
        approvalService.acceptSuggestion(suggestion2.get().getId());

        Appointment apptRebookedC = appointmentRepository.findById(appt.getId()).orElseThrow();
        assertEquals(AppointmentStatus.RESCHEDULED, apptRebookedC.getStatus());
        assertEquals(docC.getId(), apptRebookedC.getProviderId(), "Original doctor name replaced by final doctor: Doctor C");
        assertEquals(slotC.getId(), apptRebookedC.getSlotId(), "Slot assigned to Doctor C's slot");
        assertEquals(SlotStatus.BOOKED, slotRepository.findById(slotC.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Doctor becomes OFFLINE while Suggestion is PENDING: Suggestion expires and cascades to next doctor")
    void testDoctorBecomesOfflineWhileSuggestionPending() {
        // Setup 3 Dermatology doctors
        Provider docA = providerRepository.save(new Provider(null, "Dr. Dan DermA", "Dermatology", ProviderStatus.AVAILABLE));
        Provider docB = providerRepository.save(new Provider(null, "Dr. Donna DermB", "Dermatology", ProviderStatus.AVAILABLE));
        Provider docC = providerRepository.save(new Provider(null, "Dr. Derek DermC", "Dermatology", ProviderStatus.AVAILABLE));

        Room room = roomRepository.save(new Room(null, "Derm Clinic 3", "Cryo, Exam", RoomStatus.AVAILABLE));
        Patient patient = patientRepository.save(new Patient(null, "Oliver", "Twist", "(555) 123-9999", PriorityLevel.NORMAL));

        Instant baseTime = Instant.now().plus(4, ChronoUnit.HOURS);

        Slot slotA = slotRepository.save(new Slot(null, docA.getId(), room.getId(), baseTime, baseTime.plus(30, ChronoUnit.MINUTES), SlotStatus.BOOKED));
        Appointment appt = appointmentRepository.save(new Appointment(null, patient.getId(), docA.getId(), slotA.getId(), AppointmentStatus.BOOKED));

        Slot slotB = slotRepository.save(new Slot(null, docB.getId(), room.getId(), baseTime.plus(30, ChronoUnit.MINUTES), baseTime.plus(60, ChronoUnit.MINUTES), SlotStatus.AVAILABLE));
        Slot slotC = slotRepository.save(new Slot(null, docC.getId(), room.getId(), baseTime.plus(60, ChronoUnit.MINUTES), baseTime.plus(90, ChronoUnit.MINUTES), SlotStatus.AVAILABLE));

        // Doc A goes OFFLINE -> generates pending suggestion to Doc B
        disruptionService.markProviderOffline(docA.getId(), new DisruptionRequest("Offline"));
        Optional<RescheduleSuggestion> suggestionB = rescheduleOrchestrator.processSingleAppointment(appt.getId(), TriggerType.AUTO_PROVIDER_OFFLINE);
        assertTrue(suggestionB.isPresent());
        assertEquals(SuggestionStatus.PENDING, suggestionB.get().getStatus());
        assertEquals(slotB.getId(), suggestionB.get().getRecommendedSlotId());

        // NOW: Doctor B ALSO goes offline BEFORE the coordinator accepts the suggestion!
        disruptionService.markProviderOffline(docB.getId(), new DisruptionRequest("Doctor B sick"));

        // 1. Verify Doc B's pending suggestion is immediately expired
        RescheduleSuggestion expiredSugg = suggestionRepository.findById(suggestionB.get().getId()).orElseThrow();
        assertEquals(SuggestionStatus.EXPIRED, expiredSugg.getStatus());

        // 2. Trying to accept the expired suggestion throws ConflictException
        assertThrows(ConflictException.class, () -> approvalService.acceptSuggestion(suggestionB.get().getId()));

        // 3. Self-healing / reconciliation immediately finds Doctor C
        rescheduleOrchestrator.reconcileUnresolvedAppointments();

        RescheduleSuggestion suggestionC = suggestionRepository.findByAppointmentIdAndStatus(appt.getId(), SuggestionStatus.PENDING).orElseThrow();
        assertEquals(slotC.getId(), suggestionC.getRecommendedSlotId());

        // 4. Coordinator accepts Doctor C
        approvalService.acceptSuggestion(suggestionC.getId());

        Appointment finalAppt = appointmentRepository.findById(appt.getId()).orElseThrow();
        assertEquals(AppointmentStatus.RESCHEDULED, finalAppt.getStatus());
        assertEquals(docC.getId(), finalAppt.getProviderId());
    }
}
