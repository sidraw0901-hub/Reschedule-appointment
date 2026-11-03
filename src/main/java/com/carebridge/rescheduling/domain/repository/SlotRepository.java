package com.carebridge.rescheduling.domain.repository;

import com.carebridge.rescheduling.domain.model.Slot;
import com.carebridge.rescheduling.domain.model.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SlotRepository extends JpaRepository<Slot, UUID> {

    List<Slot> findByProviderIdAndStatus(UUID providerId, SlotStatus status);

    List<Slot> findByStatusAndStartTimeAfterOrderByStartTimeAsc(SlotStatus status, Instant after);

    @Query("SELECT s FROM Slot s WHERE s.providerId = :providerId AND s.startTime >= :fromTime AND s.status != 'CANCELLED'")
    List<Slot> findActiveSlotsForProviderAfter(@Param("providerId") UUID providerId, @Param("fromTime") Instant fromTime);

    @Modifying
    @Query("UPDATE Slot s SET s.status = :newStatus WHERE s.id = :id AND s.status = :expectedStatus")
    int claimSlotConditionally(@Param("id") UUID id,
                               @Param("newStatus") SlotStatus newStatus,
                               @Param("expectedStatus") SlotStatus expectedStatus);
}
