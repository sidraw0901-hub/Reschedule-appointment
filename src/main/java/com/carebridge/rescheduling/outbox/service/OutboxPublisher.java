package com.carebridge.rescheduling.outbox.service;

import com.carebridge.rescheduling.outbox.model.OutboxEvent;
import com.carebridge.rescheduling.outbox.model.OutboxStatus;
import com.carebridge.rescheduling.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public OutboxEvent publish(String aggregateType, UUID aggregateId, String eventType, Object payloadObject) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadObject);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }

        OutboxEvent event = OutboxEvent.builder()
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .eventType(eventType)
            .payload(payloadJson)
            .status(OutboxStatus.PENDING)
            .retryCount(0)
            .scheduledAt(Instant.now())
            .build();

        OutboxEvent saved = outboxEventRepository.save(event);
        log.info("Published outbox event [{}] for aggregate [{}:{}]", eventType, aggregateType, aggregateId);
        return saved;
    }
}
