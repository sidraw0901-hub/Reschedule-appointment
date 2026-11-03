package com.carebridge.rescheduling.engine.service;

import com.carebridge.rescheduling.domain.model.*;
import com.carebridge.rescheduling.domain.repository.*;
import com.carebridge.rescheduling.engine.model.RecommendationResult;
import com.carebridge.rescheduling.engine.model.RescheduleContext;
import com.carebridge.rescheduling.engine.model.SlotCandidate;
import com.carebridge.rescheduling.engine.strategy.SchedulingStrategy;
import com.carebridge.rescheduling.outbox.event.DisruptionEventPayload;
import com.carebridge.rescheduling.outbox.model.OutboxEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RescheduleOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(RescheduleOrchestrator.class);

    private final AppointmentRepository appointmentRepository;
    private final ProviderRepository providerRepository;
    private final PatientRepository patientRepository;
    private final SlotRepository slotRepository;
    private final RescheduleSuggestionRepository suggestionRepository;
    private final SlotCandidateFinder slotCandidateFinder;
    private final SchedulingStrategy schedulingStrategy;
    private final ObjectMapper objectMapper;

    public RescheduleOrchestrator(AppointmentRepository appointmentRepository,
                                  ProviderRepository providerRepository,
                                  PatientRepository patientRepository,
                                  SlotRepository slotRepository,
                                  RescheduleSuggestionRepository suggestionRepository,
                                  SlotCandidateFinder slotCandidateFinder,
                                  SchedulingStrategy schedulingStrategy,
                                  ObjectMapper objectMapper) {
        this.appointmentRepository = appointmentRepository;
        this.providerRepository = providerRepository;
        this.patientRepository = patientRepository;
        this.slotRepository = slotRepository;
        this.suggestionRepository = suggestionRepository;
        this.slotCandidateFinder = slotCandidateFinder;
        this.schedulingStrategy = schedulingStrategy;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processDisruptionEvent(OutboxEvent event) throws Exception {
        DisruptionEventPayload payload = objectMapper.readValue(event.getPayload(), DisruptionEventPayload.class);
        List<UUID> affectedIds = payload.getAffectedAppointmentIds();

        if (affectedIds == null || affectedIds.isEmpty()) {
            log.info("No affected appointments in disruption event [{}]", event.getId());
            return;
        }

        log.info("Orchestrating re-schedule recommendations for [{}] affected appointments", affectedIds.size());

        TriggerType triggerType = "PROVIDER_OFFLINE".equals(payload.getEventType())
                ? TriggerType.AUTO_PROVIDER_OFFLINE
                : TriggerType.AUTO_SLOT_CANCELLED;

        for (UUID apptId : affectedIds) {
            processSingleAppointment(apptId, triggerType);
        }
    }

    @Transactional
    public Optional<RescheduleSuggestion> processSingleAppointment(UUID appointmentId, TriggerType triggerType) {
        Appointment appt = appointmentRepository.findById(appointmentId).orElse(null);
        if (appt == null || appt.getStatus() != AppointmentStatus.RESCHEDULE_REQUIRED) {
            return Optional.empty();
        }

        // Avoid duplicate pending suggestions for the same appointment
        Optional<RescheduleSuggestion> existing = suggestionRepository.findByAppointmentIdAndStatus(appointmentId, SuggestionStatus.PENDING);
        if (existing.isPresent()) {
            return existing;
        }

        Patient patient = patientRepository.findById(appt.getPatientId()).orElse(null);
        Provider originalProvider = providerRepository.findById(appt.getProviderId()).orElse(null);
        Slot originalSlot = slotRepository.findById(appt.getSlotId()).orElse(null);

        RescheduleContext context = RescheduleContext.builder()
            .appointmentId(appt.getId())
            .patientId(appt.getPatientId())
            .patientName(patient != null ? patient.getFullName() : "Patient")
            .patientPhone(patient != null ? patient.getPhone() : "")
            .patientPriority(patient != null ? patient.getPriorityLevel().name() : "NORMAL")
            .currentProviderId(appt.getProviderId())
            .providerName(originalProvider != null ? originalProvider.getName() : "Doctor")
            .requiredSpecialty(originalProvider != null ? originalProvider.getSpecialty() : "General Medicine")
            .originalStartTime(originalSlot != null ? originalSlot.getStartTime() : null)
            .originalEndTime(originalSlot != null ? originalSlot.getEndTime() : null)
            .triggerType(triggerType)
            .build();

        List<SlotCandidate> candidates = slotCandidateFinder.findEligibleCandidates(context);

        if (candidates.isEmpty()) {
            log.warn("No eligible candidate slots found for appointment [{}]", appointmentId);
            return Optional.empty();
        }

        Optional<RecommendationResult> recOpt = schedulingStrategy.recommend(context, candidates);
        if (recOpt.isEmpty()) {
            return Optional.empty();
        }

        RecommendationResult rec = recOpt.get();

        UUID alternateProviderId = rec.getAlternateProviderId();
        if (alternateProviderId == null) {
            Slot recommendedSlot = slotRepository.findById(rec.getRecommendedSlotId()).orElse(null);
            if (recommendedSlot != null && !recommendedSlot.getProviderId().equals(appt.getProviderId())) {
                alternateProviderId = recommendedSlot.getProviderId();
            }
        }

        RescheduleSuggestion suggestion = RescheduleSuggestion.builder()
            .appointmentId(appt.getId())
            .recommendedSlotId(rec.getRecommendedSlotId())
            .alternateProviderId(alternateProviderId)
            .confidence(rec.getConfidence())
            .explanation(rec.getExplanation())
            .triggerType(triggerType)
            .strategyType(rec.getStrategyType())
            .status(SuggestionStatus.PENDING)
            .build();

        RescheduleSuggestion saved = suggestionRepository.save(suggestion);
        log.info("Persisted RescheduleSuggestion [{}] for appointment [{}] using strategy [{}]",
            saved.getId(), appt.getId(), rec.getStrategyType());

        return Optional.of(saved);
    }

    @Transactional
    public void reconcileUnresolvedAppointments() {
        List<Appointment> unresolved = appointmentRepository.findAppointmentsNeedingSuggestions(
            AppointmentStatus.RESCHEDULE_REQUIRED,
            SuggestionStatus.PENDING
        );

        if (!unresolved.isEmpty()) {
            log.info("Self-healing sweep: discovered [{}] RESCHEDULE_REQUIRED appointments without pending suggestions", unresolved.size());
            for (Appointment appt : unresolved) {
                processSingleAppointment(appt.getId(), TriggerType.AUTO_PROVIDER_OFFLINE);
            }
        }
    }
}
