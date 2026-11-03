package com.carebridge.rescheduling.outbox.repository;

import com.carebridge.rescheduling.outbox.model.OutboxEvent;
import com.carebridge.rescheduling.outbox.model.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("""
        SELECT e FROM OutboxEvent e 
        WHERE e.status = :status 
          AND e.scheduledAt <= :now 
        ORDER BY e.scheduledAt ASC
    """)
    List<OutboxEvent> findPendingEvents(@Param("status") OutboxStatus status,
                                        @Param("now") Instant now,
                                        Pageable pageable);
}
