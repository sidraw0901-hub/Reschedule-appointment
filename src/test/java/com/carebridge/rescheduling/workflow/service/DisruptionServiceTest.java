package com.carebridge.rescheduling.workflow.service;

import com.carebridge.rescheduling.domain.model.*;
import com.carebridge.rescheduling.domain.repository.AppointmentRepository;
import com.carebridge.rescheduling.domain.repository.PatientRepository;
import com.carebridge.rescheduling.domain.repository.ProviderRepository;
import com.carebridge.rescheduling.domain.repository.RoomRepository;
import com.carebridge.rescheduling.domain.repository.SlotRepository;
import com.carebridge.rescheduling.outbox.model.OutboxEvent;
import com.carebridge.rescheduling.outbox.model.OutboxStatus;
import com.carebridge.rescheduling.outbox.repository.OutboxEventRepository;
import com.carebridge.rescheduling.workflow.dto.DisruptionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DisruptionServiceTest {

    @Autowired
    private DisruptionService disruptionService;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("Provider going OFFLINE invalidates appointments and produces OutboxEvent")
    void testProviderOfflineDisruption() {
        // 1. Create a dedicated test provider, room, patient, slot, and appointment
        Provider testDoctor = providerRepository.save(Provider.builder()
            .name("Dr. Test Specialist")
            .specialty("Neurology")
            .status(ProviderStatus.AVAILABLE)
            .build());

        Room testRoom = roomRepository.save(Room.builder()
            .name("Neuro Room 5")
            .capabilities("EEG")
            .status(RoomStatus.AVAILABLE)
            .build());

        Patient testPatient = patientRepository.save(Patient.builder()
            .firstName("Alice")
            .lastName("Wonder")
            .phone("(555) 999-1111")
            .priorityLevel(PriorityLevel.HIGH)
            .build());

        Slot testSlot = slotRepository.save(Slot.builder()
            .providerId(testDoctor.getId())
            .roomId(testRoom.getId())
            .startTime(Instant.now().plusSeconds(3600))
            .endTime(Instant.now().plusSeconds(5400))
            .status(SlotStatus.BOOKED)
            .build());

        Appointment testAppt = appointmentRepository.save(Appointment.builder()
            .patientId(testPatient.getId())
            .providerId(testDoctor.getId())
            .slotId(testSlot.getId())
            .status(AppointmentStatus.BOOKED)
            .build());

        assertEquals(ProviderStatus.AVAILABLE, testDoctor.getStatus());

        // 2. Disruption: Mark test doctor offline
        DisruptionRequest request = DisruptionRequest.builder()
            .reason("Medical conference")
            .build();

        List<UUID> affectedIds = disruptionService.markProviderOffline(testDoctor.getId(), request);

        // 3. Verify provider status
        Provider updatedDoctor = providerRepository.findById(testDoctor.getId()).orElseThrow();
        assertEquals(ProviderStatus.OFFLINE, updatedDoctor.getStatus());

        // 4. Verify affected appointments
        assertTrue(affectedIds.contains(testAppt.getId()), "Should include test appointment");
        assertEquals(AppointmentStatus.RESCHEDULE_REQUIRED,
            appointmentRepository.findById(testAppt.getId()).orElseThrow().getStatus());

        // 5. Verify Outbox Event created
        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        boolean foundDisruptionEvent = outboxEvents.stream()
            .anyMatch(e -> "PROVIDER_OFFLINE".equals(e.getEventType()) &&
                           e.getAggregateId().equals(testDoctor.getId()) &&
                           e.getStatus() == OutboxStatus.PENDING);

        assertTrue(foundDisruptionEvent, "Expected PENDING PROVIDER_OFFLINE outbox event");
    }
}
