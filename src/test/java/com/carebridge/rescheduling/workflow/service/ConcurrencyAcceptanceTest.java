package com.carebridge.rescheduling.workflow.service;

import com.carebridge.rescheduling.common.exception.ConflictException;
import com.carebridge.rescheduling.domain.model.*;
import com.carebridge.rescheduling.domain.repository.AppointmentRepository;
import com.carebridge.rescheduling.domain.repository.RescheduleSuggestionRepository;
import com.carebridge.rescheduling.domain.repository.SlotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConcurrencyAcceptanceTest {

    @Autowired
    private SuggestionApprovalService approvalService;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private RescheduleSuggestionRepository suggestionRepository;

    @Autowired
    private com.carebridge.rescheduling.domain.repository.ProviderRepository providerRepository;

    @Test
    @DisplayName("Two coordinators accepting suggestions for the SAME target slot simultaneously: Exactly ONE succeeds, ONE fails with ConflictException")
    void testConcurrentAcceptanceRaceCondition() throws Exception {
        // 1. Create a test doctor and single contested available slot
        Provider testDoc = providerRepository.save(new Provider(null, "Dr. Concurrency Specialist", "Cardiology", ProviderStatus.AVAILABLE));

        Slot targetSlot = slotRepository.save(Slot.builder()
            .providerId(testDoc.getId())
            .roomId(UUID.randomUUID())
            .startTime(Instant.now().plusSeconds(3600))
            .endTime(Instant.now().plusSeconds(5400))
            .status(SlotStatus.AVAILABLE)
            .build());

        // 2. Create two disrupted appointments
        Appointment appt1 = appointmentRepository.save(Appointment.builder()
            .patientId(UUID.randomUUID())
            .providerId(UUID.randomUUID())
            .slotId(UUID.randomUUID())
            .status(AppointmentStatus.RESCHEDULE_REQUIRED)
            .build());

        Appointment appt2 = appointmentRepository.save(Appointment.builder()
            .patientId(UUID.randomUUID())
            .providerId(UUID.randomUUID())
            .slotId(UUID.randomUUID())
            .status(AppointmentStatus.RESCHEDULE_REQUIRED)
            .build());

        // 3. Create two pending suggestions both pointing to the same contested targetSlot
        RescheduleSuggestion sugg1 = suggestionRepository.save(RescheduleSuggestion.builder()
            .appointmentId(appt1.getId())
            .recommendedSlotId(targetSlot.getId())
            .confidence(new BigDecimal("0.95"))
            .explanation("Option for Patient 1")
            .triggerType(TriggerType.AUTO_PROVIDER_OFFLINE)
            .strategyType(StrategyType.AI_POWERED)
            .status(SuggestionStatus.PENDING)
            .build());

        RescheduleSuggestion sugg2 = suggestionRepository.save(RescheduleSuggestion.builder()
            .appointmentId(appt2.getId())
            .recommendedSlotId(targetSlot.getId())
            .confidence(new BigDecimal("0.91"))
            .explanation("Option for Patient 2")
            .triggerType(TriggerType.AUTO_PROVIDER_OFFLINE)
            .strategyType(StrategyType.AI_POWERED)
            .status(SuggestionStatus.PENDING)
            .build());

        // 4. Simulate two coordinators clicking 'Accept' simultaneously
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        Callable<Void> task1 = () -> {
            barrier.await();
            try {
                approvalService.acceptSuggestion(sugg1.getId());
                successCount.incrementAndGet();
            } catch (ConflictException e) {
                conflictCount.incrementAndGet();
            }
            return null;
        };

        Callable<Void> task2 = () -> {
            barrier.await();
            try {
                approvalService.acceptSuggestion(sugg2.getId());
                successCount.incrementAndGet();
            } catch (ConflictException e) {
                conflictCount.incrementAndGet();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(task1);
        Future<Void> f2 = executor.submit(task2);

        f1.get(5, TimeUnit.SECONDS);
        f2.get(5, TimeUnit.SECONDS);
        executor.shutdown();

        // 5. Assertions: Mathematical guarantee that double booking did not happen
        assertEquals(1, successCount.get(), "Exactly one coordinator must succeed");
        assertEquals(1, conflictCount.get(), "Competing coordinator must receive ConflictException");

        // Slot must be BOOKED
        Slot finalSlot = slotRepository.findById(targetSlot.getId()).orElseThrow();
        assertEquals(SlotStatus.BOOKED, finalSlot.getStatus());

        // One suggestion must be ACCEPTED, the other must be EXPIRED
        RescheduleSuggestion s1Final = suggestionRepository.findById(sugg1.getId()).orElseThrow();
        RescheduleSuggestion s2Final = suggestionRepository.findById(sugg2.getId()).orElseThrow();

        assertTrue(
            (s1Final.getStatus() == SuggestionStatus.ACCEPTED && s2Final.getStatus() == SuggestionStatus.EXPIRED) ||
            (s2Final.getStatus() == SuggestionStatus.ACCEPTED && s1Final.getStatus() == SuggestionStatus.EXPIRED),
            "One suggestion must be ACCEPTED and the other EXPIRED"
        );
    }
}
