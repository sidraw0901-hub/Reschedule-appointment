package com.carebridge.rescheduling.engine.strategy;

import com.carebridge.rescheduling.domain.model.StrategyType;
import com.carebridge.rescheduling.engine.model.RecommendationResult;
import com.carebridge.rescheduling.engine.model.RescheduleContext;
import com.carebridge.rescheduling.engine.model.SlotCandidate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RuleBasedSchedulingStrategy implements SchedulingStrategy {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a")
            .withZone(ZoneId.systemDefault());

    @Override
    public StrategyType getStrategyType() {
        return StrategyType.RULE_BASED;
    }

    @Override
    public Optional<RecommendationResult> recommend(RescheduleContext context, List<SlotCandidate> eligibleCandidates) {
        if (eligibleCandidates == null || eligibleCandidates.isEmpty()) {
            return Optional.empty();
        }

        // Deterministic scoring: prefer same provider (+100) and closest time slot
        SlotCandidate bestCandidate = eligibleCandidates.stream()
            .max(Comparator.comparingDouble(candidate -> {
                double score = 1000.0;
                if (candidate.isOriginalProvider()) {
                    score += 200.0;
                }
                // Deduct 1 point per 10 minutes difference
                score -= (candidate.getMinutesDifferenceFromOriginal() / 10.0);
                return score;
            }))
            .orElse(eligibleCandidates.get(0));

        UUID alternateProviderId = bestCandidate.isOriginalProvider() ? null : bestCandidate.getProviderId();
        BigDecimal confidence = bestCandidate.isOriginalProvider() ? new BigDecimal("0.98") : new BigDecimal("0.85");

        String timeStr = TIME_FMT.format(bestCandidate.getStartTime());
        String script;
        if (bestCandidate.isOriginalProvider()) {
            script = String.format("Hello %s, your appointment has been updated to %s in %s with %s.",
                context.getPatientName() != null ? context.getPatientName() : "there",
                timeStr,
                bestCandidate.getRoomName(),
                bestCandidate.getProviderName());
        } else {
            script = String.format("Hello %s, %s had an unforeseen disruption. We have arranged an appointment for you today at %s with colleague %s in %s. Does that time work for you?",
                context.getPatientName() != null ? context.getPatientName() : "there",
                context.getProviderName() != null ? context.getProviderName() : "your doctor",
                timeStr,
                bestCandidate.getProviderName(),
                bestCandidate.getRoomName());
        }

        String explanation = String.format("Rule-based match: Selected %s at %s (%d mins difference from original) in %s.",
            bestCandidate.getProviderName(),
            timeStr,
            bestCandidate.getMinutesDifferenceFromOriginal(),
            bestCandidate.getRoomName());

        return Optional.of(RecommendationResult.builder()
            .recommendedSlotId(bestCandidate.getSlotId())
            .alternateProviderId(alternateProviderId)
            .confidence(confidence)
            .explanation(script + " [" + explanation + "]")
            .strategyType(StrategyType.RULE_BASED)
            .build());
    }
}
