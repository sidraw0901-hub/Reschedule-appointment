package com.carebridge.rescheduling.e2e;

import com.carebridge.rescheduling.domain.model.*;
import com.carebridge.rescheduling.domain.repository.*;
import com.carebridge.rescheduling.outbox.service.OutboxPollerWorker;
import com.carebridge.rescheduling.workflow.dto.DisruptionRequest;
import com.carebridge.rescheduling.workflow.service.DisruptionService;
import com.carebridge.rescheduling.workflow.service.SuggestionApprovalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@org.springframework.test.annotation.DirtiesContext
class EndToEndReactiveLoopTest {

    @Autowired
    private DisruptionService disruptionService;

    @Autowired
    private OutboxPollerWorker outboxPollerWorker;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private RescheduleSuggestionRepository suggestionRepository;

    @Autowired
    private SuggestionApprovalService approvalService;

    @Autowired
    private SlotRepository slotRepository;

    @Test
    @DisplayName("Complete E2E Loop: Disruption -> Outbox Worker -> AI/Rule Suggestions -> Staff Approval -> Atomic Rebook")
    void testCompleteRescheduleLoop() {
        // Step 1: Identify Dr. Sarah Chen
        Provider drChen = providerRepository.findAll().stream()
            .filter(p -> p.getName().contains("Chen"))
            .findFirst()
            .orElseThrow();

        // Step 2: Trigger Disruption (Provider OFFLINE)
        List<UUID> affectedApptIds = disruptionService.markProviderOffline(
            drChen.getId(),
            new DisruptionRequest("Flu emergency", null)
        );

        assertFalse(affectedApptIds.isEmpty(), "Should have affected appointments for Dr. Chen");

        // Step 3: Trigger Background Outbox Worker
        outboxPollerWorker.pollAndProcessOutboxEvents();

        // Step 4: Verify Suggestions were generated
        List<RescheduleSuggestion> pendingSuggestions = suggestionRepository.findByStatus(SuggestionStatus.PENDING);
        assertFalse(pendingSuggestions.isEmpty(), "Worker should have produced PENDING reschedule suggestions");

        RescheduleSuggestion firstSuggestion = pendingSuggestions.get(0);
        assertNotNull(firstSuggestion.getRecommendedSlotId());
        assertNotNull(firstSuggestion.getExplanation());
        assertTrue(firstSuggestion.getExplanation().length() > 10);

        // Step 5: Coordinator Reviews & Accepts the Suggestion
        approvalService.acceptSuggestion(firstSuggestion.getId());

        // Step 6: Verify Rescheduling Outcome
        Appointment rebookedAppt = appointmentRepository.findById(firstSuggestion.getAppointmentId()).orElseThrow();
        assertEquals(AppointmentStatus.RESCHEDULED, rebookedAppt.getStatus());
        assertEquals(firstSuggestion.getRecommendedSlotId(), rebookedAppt.getSlotId());

        Slot rebookedSlot = slotRepository.findById(firstSuggestion.getRecommendedSlotId()).orElseThrow();
        assertEquals(SlotStatus.BOOKED, rebookedSlot.getStatus());

        RescheduleSuggestion updatedSuggestion = suggestionRepository.findById(firstSuggestion.getId()).orElseThrow();
        assertEquals(SuggestionStatus.ACCEPTED, updatedSuggestion.getStatus());
    }
}
