package com.carebridge.rescheduling.engine.strategy;

import com.carebridge.rescheduling.domain.model.StrategyType;
import com.carebridge.rescheduling.domain.model.TriggerType;
import com.carebridge.rescheduling.engine.model.RecommendationResult;
import com.carebridge.rescheduling.engine.model.RescheduleContext;
import com.carebridge.rescheduling.engine.model.SlotCandidate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedStrategyTest {

    private final RuleBasedSchedulingStrategy strategy = new RuleBasedSchedulingStrategy();

    @Test
    @DisplayName("Should rank same provider and closer time higher")
    void testRuleBasedRanking() {
        UUID origProviderId = UUID.randomUUID();
        UUID altProviderId = UUID.randomUUID();
        Instant now = Instant.now();

        RescheduleContext context = RescheduleContext.builder()
            .appointmentId(UUID.randomUUID())
            .patientName("Eleanor Vance")
            .currentProviderId(origProviderId)
            .providerName("Dr. Sarah Chen")
            .requiredSpecialty("Cardiology")
            .originalStartTime(now)
            .triggerType(TriggerType.AUTO_PROVIDER_OFFLINE)
            .build();

        SlotCandidate cand1 = SlotCandidate.builder()
            .slotId(UUID.randomUUID())
            .providerId(altProviderId)
            .providerName("Dr. Marcus Brody")
            .specialty("Cardiology")
            .originalProvider(false)
            .roomName("Room 2")
            .startTime(now.plusSeconds(3600))
            .minutesDifferenceFromOriginal(60)
            .build();

        SlotCandidate cand2 = SlotCandidate.builder()
            .slotId(UUID.randomUUID())
            .providerId(origProviderId)
            .providerName("Dr. Sarah Chen")
            .specialty("Cardiology")
            .originalProvider(true)
            .roomName("Room 1A")
            .startTime(now.plusSeconds(7200))
            .minutesDifferenceFromOriginal(120)
            .build();

        Optional<RecommendationResult> rec = strategy.recommend(context, List.of(cand1, cand2));

        assertTrue(rec.isPresent());
        // Same provider should win despite being 60 mins later due to +200 bonus
        assertEquals(cand2.getSlotId(), rec.get().getRecommendedSlotId());
        assertEquals(StrategyType.RULE_BASED, rec.get().getStrategyType());
        assertTrue(rec.get().getExplanation().contains("Eleanor Vance"));
    }
}
