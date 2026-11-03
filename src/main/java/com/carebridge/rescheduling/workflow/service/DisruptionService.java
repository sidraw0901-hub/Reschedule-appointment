package com.carebridge.rescheduling.workflow.service;

import com.carebridge.rescheduling.common.exception.ResourceNotFoundException;
import com.carebridge.rescheduling.domain.model.*;
import com.carebridge.rescheduling.domain.repository.AppointmentRepository;
import com.carebridge.rescheduling.domain.repository.ProviderRepository;
import com.carebridge.rescheduling.domain.repository.RescheduleSuggestionRepository;
import com.carebridge.rescheduling.domain.repository.SlotRepository;
import com.carebridge.rescheduling.outbox.event.DisruptionEventPayload;
import com.carebridge.rescheduling.outbox.service.OutboxPublisher;
import com.carebridge.rescheduling.workflow.dto.DisruptionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DisruptionService {

    private static final Logger log = LoggerFactory.getLogger(DisruptionService.class);

    private final ProviderRepository providerRepository;
    private final SlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final RescheduleSuggestionRepository suggestionRepository;
    private final OutboxPublisher outboxPublisher;

    public DisruptionService(ProviderRepository providerRepository,
                             SlotRepository slotRepository,
                             AppointmentRepository appointmentRepository,
                             RescheduleSuggestionRepository suggestionRepository,
                             OutboxPublisher outboxPublisher) {
        this.providerRepository = providerRepository;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.suggestionRepository = suggestionRepository;
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional
    public List<UUID> markProviderOffline(UUID providerId, DisruptionRequest request) {
        Provider provider = providerRepository.findById(providerId)
            .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + providerId));

        provider.setStatus(ProviderStatus.OFFLINE);
        providerRepository.save(provider);

        // 1. Identify all active appointments for this provider (both BOOKED and previously RESCHEDULED)
        Instant now = Instant.now();
        List<Appointment> affectedAppointments = appointmentRepository.findFutureBookedAppointmentsForProvider(providerId, now);

        List<UUID> affectedIds = new ArrayList<>();
        for (Appointment appt : affectedAppointments) {
            appt.setStatus(AppointmentStatus.RESCHEDULE_REQUIRED);
            appointmentRepository.save(appt);
            affectedIds.add(appt.getId());

            slotRepository.findById(appt.getSlotId()).ifPresent(slot -> {
                slot.setStatus(SlotStatus.CANCELLED);
                slotRepository.save(slot);
            });
        }

        // 2. Critical Edge Case: Expire any pending suggestions recommending this doctor who just went offline!
        // Also queue those affected appointments so an alternate available doctor (Doctor C) is immediately assigned!
        List<RescheduleSuggestion> pendingForOfflineDoctor = suggestionRepository.findPendingSuggestionsForProvider(providerId);
        for (RescheduleSuggestion s : pendingForOfflineDoctor) {
            s.setStatus(SuggestionStatus.EXPIRED);
            suggestionRepository.save(s);
            if (!affectedIds.contains(s.getAppointmentId())) {
                affectedIds.add(s.getAppointmentId());
            }
        }
        if (!pendingForOfflineDoctor.isEmpty()) {
            log.info("Expired [{}] pending suggestions recommending offline doctor [{}] and re-queued appointments for alternate doctors",
                pendingForOfflineDoctor.size(), provider.getName());
        }

        // 3. Write outbox event so autonomous worker immediately finds an alternative doctor (Doctor C)
        DisruptionEventPayload payload = DisruptionEventPayload.builder()
            .eventType("PROVIDER_OFFLINE")
            .providerId(providerId)
            .reason(request != null && request.getReason() != null ? request.getReason() : "Provider marked offline")
            .affectedAppointmentIds(affectedIds)
            .occurredAt(Instant.now())
            .build();

        outboxPublisher.publish("PROVIDER", providerId, "PROVIDER_OFFLINE", payload);

        log.info("Provider [{}] marked OFFLINE. [{}] appointments marked RESCHEDULE_REQUIRED.", provider.getName(), affectedIds.size());
        return affectedIds;
    }

    @Transactional
    public void restoreProvider(UUID providerId) {
        Provider provider = providerRepository.findById(providerId)
            .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + providerId));

        provider.setStatus(ProviderStatus.AVAILABLE);
        providerRepository.save(provider);

        // Re-open cancelled slots so doctor can accept appointments again
        List<Slot> cancelledSlots = slotRepository.findByProviderIdAndStatus(providerId, SlotStatus.CANCELLED);
        for (Slot slot : cancelledSlots) {
            slot.setStatus(SlotStatus.AVAILABLE);
            slotRepository.save(slot);
        }

        log.info("Provider [{}] restored to AVAILABLE status. Re-opened [{}] slots for booking so they can accept appointments.",
            provider.getName(), cancelledSlots.size());
    }

    @Transactional
    public void cancelSlot(UUID slotId, String reason) {
        Slot slot = slotRepository.findById(slotId)
            .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));

        slot.setStatus(SlotStatus.CANCELLED);
        slotRepository.save(slot);

        List<UUID> affectedIds = new ArrayList<>();
        appointmentRepository.findBySlotId(slotId).ifPresent(appt -> {
            if (appt.getStatus() == AppointmentStatus.BOOKED || appt.getStatus() == AppointmentStatus.RESCHEDULED) {
                appt.setStatus(AppointmentStatus.RESCHEDULE_REQUIRED);
                appointmentRepository.save(appt);
                affectedIds.add(appt.getId());
            }
        });

        DisruptionEventPayload payload = DisruptionEventPayload.builder()
            .eventType("SLOT_CANCELLED")
            .slotId(slotId)
            .providerId(slot.getProviderId())
            .reason(reason != null ? reason : "Slot cancelled by administrator")
            .affectedAppointmentIds(affectedIds)
            .occurredAt(Instant.now())
            .build();

        outboxPublisher.publish("SLOT", slotId, "SLOT_CANCELLED", payload);
        log.info("Slot [{}] CANCELLED. [{}] appointments affected.", slotId, affectedIds.size());
    }
}
