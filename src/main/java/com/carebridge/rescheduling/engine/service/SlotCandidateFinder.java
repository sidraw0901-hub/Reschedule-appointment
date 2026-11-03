package com.carebridge.rescheduling.engine.service;

import com.carebridge.rescheduling.domain.model.*;
import com.carebridge.rescheduling.domain.repository.ProviderRepository;
import com.carebridge.rescheduling.domain.repository.RoomRepository;
import com.carebridge.rescheduling.domain.repository.SlotRepository;
import com.carebridge.rescheduling.engine.model.RescheduleContext;
import com.carebridge.rescheduling.engine.model.SlotCandidate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SlotCandidateFinder {

    private final SlotRepository slotRepository;
    private final ProviderRepository providerRepository;
    private final RoomRepository roomRepository;

    public SlotCandidateFinder(SlotRepository slotRepository,
                               ProviderRepository providerRepository,
                               RoomRepository roomRepository) {
        this.slotRepository = slotRepository;
        this.providerRepository = providerRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public List<SlotCandidate> findEligibleCandidates(RescheduleContext context) {
        Instant now = Instant.now();
        List<Slot> availableSlots = slotRepository.findByStatusAndStartTimeAfterOrderByStartTimeAsc(SlotStatus.AVAILABLE, now);

        Map<UUID, Provider> providerMap = providerRepository.findAll().stream()
            .collect(Collectors.toMap(Provider::getId, p -> p));

        Map<UUID, Room> roomMap = roomRepository.findAll().stream()
            .collect(Collectors.toMap(Room::getId, r -> r));

        List<SlotCandidate> candidates = new ArrayList<>();

        for (Slot slot : availableSlots) {
            Provider provider = providerMap.get(slot.getProviderId());
            Room room = roomMap.get(slot.getRoomId());

            if (provider == null || room == null) {
                continue;
            }

            // Hard constraint 1: Room must be operational
            if (room.getStatus() != RoomStatus.AVAILABLE) {
                continue;
            }

            // Hard constraint 2: Specialty must match
            if (!provider.getSpecialty().equalsIgnoreCase(context.getRequiredSpecialty())) {
                continue;
            }

            // Hard constraint 3: If provider is marked OFFLINE, cannot book slots with that provider during outage
            // (Unless the slot is far in future, but for safety in MVP, provider must be AVAILABLE)
            if (provider.getStatus() == ProviderStatus.OFFLINE) {
                continue;
            }

            boolean isOriginalProvider = provider.getId().equals(context.getCurrentProviderId());
            long minutesDiff = 0;
            if (context.getOriginalStartTime() != null) {
                minutesDiff = Math.abs(Duration.between(context.getOriginalStartTime(), slot.getStartTime()).toMinutes());
            }

            candidates.add(SlotCandidate.builder()
                .slotId(slot.getId())
                .providerId(provider.getId())
                .providerName(provider.getName())
                .specialty(provider.getSpecialty())
                .originalProvider(isOriginalProvider)
                .roomId(room.getId())
                .roomName(room.getName())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .minutesDifferenceFromOriginal(minutesDiff)
                .build());
        }

        // Return sorted primarily by proximity to original time
        candidates.sort(Comparator.comparingLong(SlotCandidate::getMinutesDifferenceFromOriginal));
        return candidates;
    }
}
