package com.carebridge.rescheduling.outbox.service;

import com.carebridge.rescheduling.engine.service.RescheduleOrchestrator;
import com.carebridge.rescheduling.outbox.model.OutboxEvent;
import com.carebridge.rescheduling.outbox.model.OutboxStatus;
import com.carebridge.rescheduling.outbox.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxPollerWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxPollerWorker.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RescheduleOrchestrator rescheduleOrchestrator;

    public OutboxPollerWorker(OutboxEventRepository outboxEventRepository,
                              RescheduleOrchestrator rescheduleOrchestrator) {
        this.outboxEventRepository = outboxEventRepository;
        this.rescheduleOrchestrator = rescheduleOrchestrator;
    }

    @Scheduled(fixedDelayString = "${carebridge.outbox.poll-interval-ms:1000}")
    @Transactional
    public void pollAndProcessOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(
            OutboxStatus.PENDING,
            Instant.now(),
            PageRequest.of(0, 10)
        );

        if (!pendingEvents.isEmpty()) {
            log.debug("Outbox worker discovered [{}] pending disruption events", pendingEvents.size());

            for (OutboxEvent event : pendingEvents) {
                try {
                    event.setStatus(OutboxStatus.PROCESSING);
                    outboxEventRepository.saveAndFlush(event);

                    rescheduleOrchestrator.processDisruptionEvent(event);

                    event.setStatus(OutboxStatus.PROCESSED);
                    event.setProcessedAt(Instant.now());
                    outboxEventRepository.save(event);
                    log.info("Successfully processed outbox event [{}] for aggregate [{}:{}]",
                        event.getId(), event.getAggregateType(), event.getAggregateId());

                } catch (Exception ex) {
                    log.error("Error processing outbox event [{}]", event.getId(), ex);
                    int retries = event.getRetryCount() + 1;
                    event.setRetryCount(retries);
                    event.setErrorMessage(ex.getMessage());

                    if (retries >= 3) {
                        event.setStatus(OutboxStatus.DEAD_LETTER);
                    } else {
                        event.setStatus(OutboxStatus.PENDING);
                        event.setScheduledAt(Instant.now().plusSeconds(retries * 2L));
                    }
                    outboxEventRepository.save(event);
                }
            }
        }

        // Self-healing check: guarantee that any orphaned RESCHEDULE_REQUIRED appointments (e.g. from cascading doctor disruptions) are resolved
        try {
            rescheduleOrchestrator.reconcileUnresolvedAppointments();
        } catch (Exception e) {
            log.error("Error during unresolved appointment reconciliation", e);
        }
    }
}
