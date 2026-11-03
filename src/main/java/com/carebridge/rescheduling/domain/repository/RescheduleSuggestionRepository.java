package com.carebridge.rescheduling.domain.repository;

import com.carebridge.rescheduling.domain.model.RescheduleSuggestion;
import com.carebridge.rescheduling.domain.model.SuggestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RescheduleSuggestionRepository extends JpaRepository<RescheduleSuggestion, UUID> {

    List<RescheduleSuggestion> findByStatus(SuggestionStatus status);

    List<RescheduleSuggestion> findByAppointmentId(UUID appointmentId);

    Optional<RescheduleSuggestion> findByAppointmentIdAndStatus(UUID appointmentId, SuggestionStatus status);

    @Modifying
    @Query("""
        UPDATE RescheduleSuggestion s 
        SET s.status = 'EXPIRED' 
        WHERE s.recommendedSlotId = :slotId 
          AND s.id != :winningSuggestionId 
          AND s.status = 'PENDING'
    """)
    int expireCompetingSuggestions(@Param("slotId") UUID slotId, @Param("winningSuggestionId") UUID winningSuggestionId);

    @Modifying
    @Query("""
        UPDATE RescheduleSuggestion s 
        SET s.status = 'EXPIRED' 
        WHERE s.recommendedSlotId IN (SELECT sl.id FROM Slot sl WHERE sl.providerId = :providerId) 
          AND s.status = 'PENDING'
    """)
    int expirePendingSuggestionsForProvider(@Param("providerId") UUID providerId);

    @Query("""
        SELECT s FROM RescheduleSuggestion s 
        WHERE s.recommendedSlotId IN (SELECT sl.id FROM Slot sl WHERE sl.providerId = :providerId) 
          AND s.status = 'PENDING'
    """)
    List<RescheduleSuggestion> findPendingSuggestionsForProvider(@Param("providerId") UUID providerId);
}
