package com.carebridge.rescheduling.engine.strategy;

import com.carebridge.rescheduling.domain.model.StrategyType;
import com.carebridge.rescheduling.engine.model.RecommendationResult;
import com.carebridge.rescheduling.engine.model.RescheduleContext;
import com.carebridge.rescheduling.engine.model.SlotCandidate;

import java.util.List;
import java.util.Optional;

public interface SchedulingStrategy {
    StrategyType getStrategyType();
    Optional<RecommendationResult> recommend(RescheduleContext context, List<SlotCandidate> eligibleCandidates);
}
