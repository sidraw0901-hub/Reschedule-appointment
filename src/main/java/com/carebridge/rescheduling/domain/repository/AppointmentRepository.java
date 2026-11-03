package com.carebridge.rescheduling.domain.repository;

import com.carebridge.rescheduling.domain.model.Appointment;
import com.carebridge.rescheduling.domain.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByStatus(AppointmentStatus status);

    List<Appointment> findByProviderIdAndStatus(UUID providerId, AppointmentStatus status);

    Optional<Appointment> findBySlotId(UUID slotId);

    @Query("""
        SELECT a FROM Appointment a 
        JOIN Slot s ON a.slotId = s.id 
        WHERE a.providerId = :providerId 
          AND (a.status = 'BOOKED' OR a.status = 'RESCHEDULED') 
          AND s.startTime >= :fromTime
    """)
    List<Appointment> findFutureBookedAppointmentsForProvider(@Param("providerId") UUID providerId, @Param("fromTime") Instant fromTime);

    @Query("""
        SELECT a FROM Appointment a 
        WHERE a.status = :status 
          AND NOT EXISTS (
              SELECT s FROM RescheduleSuggestion s 
              WHERE s.appointmentId = a.id 
                AND s.status = :suggStatus
          )
    """)
    List<Appointment> findAppointmentsNeedingSuggestions(
        @Param("status") AppointmentStatus status, 
        @Param("suggStatus") com.carebridge.rescheduling.domain.model.SuggestionStatus suggStatus
    );
}
