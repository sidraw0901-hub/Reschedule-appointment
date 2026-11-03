package com.carebridge.rescheduling.workflow.service;

import com.carebridge.rescheduling.common.exception.ConflictException;
import com.carebridge.rescheduling.common.exception.ResourceNotFoundException;
import com.carebridge.rescheduling.domain.model.*;
import com.carebridge.rescheduling.domain.repository.AppointmentRepository;
import com.carebridge.rescheduling.domain.repository.ProviderRepository;
import com.carebridge.rescheduling.domain.repository.RescheduleSuggestionRepository;
import com.carebridge.rescheduling.domain.repository.SlotRepository;
import com.carebridge.rescheduling.workflow.dto.RebookResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SuggestionApprovalService {

    private static final Logger log = LoggerFactory.getLogger(SuggestionApprovalService.class);

    private final RescheduleSuggestionRepository suggestionRepository;
    private final SlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProviderRepository providerRepository;
    private final com.carebridge.rescheduling.engine.service.RescheduleOrchestrator rescheduleOrchestrator;

    public SuggestionApprovalService(RescheduleSuggestionRepository suggestionRepository,
                                     SlotRepository slotRepository,
                                     AppointmentRepository appointmentRepository,
                                     ProviderRepository providerRepository,
                                     com.carebridge.rescheduling.engine.service.RescheduleOrchestrator rescheduleOrchestrator) {
        this.suggestionRepository = suggestionRepository;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.providerRepository = providerRepository;
        this.rescheduleOrchestrator = rescheduleOrchestrator;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RebookResultDTO acceptSuggestion(UUID suggestionId) {
        RescheduleSuggestion suggestion = suggestionRepository.findById(suggestionId)
            .orElseThrow(() -> new ResourceNotFoundException("Reschedule suggestion not found: " + suggestionId));

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new ConflictException("Suggestion is no longer pending. Current status: " + suggestion.getStatus());
        }

        UUID targetSlotId = suggestion.getRecommendedSlotId();
        UUID appointmentId = suggestion.getAppointmentId();

        // 1. Fetch target slot
        Slot targetSlot = slotRepository.findById(targetSlotId)
            .orElseThrow(() -> new ResourceNotFoundException("Target slot not found: " + targetSlotId));

        // 2. EDGE CASE CHECK: What if the receiving doctor who is taking this appointment ALSO became unavailable?
        Provider targetDoctor = providerRepository.findById(targetSlot.getProviderId())
            .orElseThrow(() -> new IllegalStateException("Provider not found for slot: " + targetSlotId));

        if (targetDoctor.getStatus() == ProviderStatus.OFFLINE) {
            suggestion.setStatus(SuggestionStatus.EXPIRED);
            suggestionRepository.save(suggestion);
            log.warn("Secondary disruption detected: Receiving doctor [{}] is OFFLINE. Expired suggestion [{}]",
                targetDoctor.getName(), suggestionId);

            // Re-route appointment immediately to next eligible doctor (e.g. Doctor C)
            rescheduleOrchestrator.processSingleAppointment(appointmentId, TriggerType.AUTO_PROVIDER_OFFLINE);

            throw new ConflictException("The receiving physician (" + targetDoctor.getName() + ") has just become unavailable. Suggestion expired; searching for an alternate available doctor.");
        }

        // 3. Atomic CAS update on the slot: guarantees single-winner concurrency protection
        int rowsUpdated = slotRepository.claimSlotConditionally(
            targetSlotId,
            SlotStatus.BOOKED,
            SlotStatus.AVAILABLE
        );

        if (rowsUpdated == 0) {
            suggestion.setStatus(SuggestionStatus.EXPIRED);
            suggestionRepository.save(suggestion);
            log.warn("Double-booking prevented: Slot [{}] was already claimed. Expiring suggestion [{}]",
                targetSlotId, suggestionId);
            throw new ConflictException("The proposed slot was already booked by another coordinator. Suggestion has been expired.");
        }

        // 4. Atomically update Appointment to the receiving doctor
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new IllegalStateException("Appointment missing: " + appointmentId));

        appointment.setSlotId(targetSlotId);
        appointment.setProviderId(targetDoctor.getId());
        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        appointmentRepository.save(appointment);

        // 5. Mark this suggestion ACCEPTED
        suggestion.setStatus(SuggestionStatus.ACCEPTED);
        suggestionRepository.save(suggestion);

        // 6. Expire all other pending suggestions targeting this exact slot
        int expiredCount = suggestionRepository.expireCompetingSuggestions(targetSlotId, suggestionId);
        log.info("Accepted suggestion [{}]. Appointment [{}] rescheduled to doctor [{}] at slot [{}]. Expired [{}] competing suggestions.",
            suggestionId, appointmentId, targetDoctor.getName(), targetSlotId, expiredCount);

        return new RebookResultDTO(
            appointmentId,
            targetSlotId,
            AppointmentStatus.RESCHEDULED,
            SuggestionStatus.ACCEPTED,
            "Appointment successfully rescheduled to " + targetDoctor.getName()
        );
    }

    @Transactional
    public void rejectSuggestion(UUID suggestionId, String reason) {
        RescheduleSuggestion suggestion = suggestionRepository.findById(suggestionId)
            .orElseThrow(() -> new ResourceNotFoundException("Reschedule suggestion not found: " + suggestionId));

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new ConflictException("Cannot reject suggestion with status: " + suggestion.getStatus());
        }

        suggestion.setStatus(SuggestionStatus.REJECTED);
        suggestionRepository.save(suggestion);
        log.info("Coordinator rejected suggestion [{}] with reason: {}", suggestionId, reason);
    }
}
