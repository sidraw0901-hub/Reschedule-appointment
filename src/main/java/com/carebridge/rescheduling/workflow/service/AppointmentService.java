package com.carebridge.rescheduling.workflow.service;

import com.carebridge.rescheduling.common.exception.ConflictException;
import com.carebridge.rescheduling.common.exception.ResourceNotFoundException;
import com.carebridge.rescheduling.domain.model.*;
import com.carebridge.rescheduling.domain.repository.*;
import com.carebridge.rescheduling.workflow.dto.AppointmentDetailDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ProviderRepository providerRepository;
    private final SlotRepository slotRepository;
    private final PatientRepository patientRepository;
    private final RoomRepository roomRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              ProviderRepository providerRepository,
                              SlotRepository slotRepository,
                              PatientRepository patientRepository,
                              RoomRepository roomRepository) {
        this.appointmentRepository = appointmentRepository;
        this.providerRepository = providerRepository;
        this.slotRepository = slotRepository;
        this.patientRepository = patientRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public List<AppointmentDetailDTO> getAllAppointments() {
        return appointmentRepository.findAll().stream()
            .map(this::toDetailDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentDetailDTO> getAffectedAppointments() {
        return appointmentRepository.findByStatus(AppointmentStatus.RESCHEDULE_REQUIRED).stream()
            .map(this::toDetailDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentDetailDTO bookAppointment(UUID patientId, UUID providerId, UUID slotId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));

        Provider provider = providerRepository.findById(providerId)
            .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + providerId));

        Slot slot = slotRepository.findById(slotId)
            .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new ConflictException("Slot is not available for booking (Status: " + slot.getStatus() + ")");
        }

        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);

        Appointment appt = Appointment.builder()
            .patientId(patient.getId())
            .providerId(provider.getId())
            .slotId(slot.getId())
            .status(AppointmentStatus.BOOKED)
            .build();

        Appointment saved = appointmentRepository.save(appt);
        return toDetailDTO(saved);
    }

    private AppointmentDetailDTO toDetailDTO(Appointment appt) {
        Patient patient = patientRepository.findById(appt.getPatientId()).orElse(null);
        Provider provider = providerRepository.findById(appt.getProviderId()).orElse(null);
        Slot slot = slotRepository.findById(appt.getSlotId()).orElse(null);
        Room room = (slot != null) ? roomRepository.findById(slot.getRoomId()).orElse(null) : null;

        return AppointmentDetailDTO.builder()
            .appointmentId(appt.getId())
            .patientId(appt.getPatientId())
            .patientName(patient != null ? patient.getFullName() : "Unknown")
            .patientPhone(patient != null ? patient.getPhone() : "")
            .priorityLevel(patient != null ? patient.getPriorityLevel().name() : "NORMAL")
            .providerId(appt.getProviderId())
            .providerName(provider != null ? provider.getName() : "Unknown")
            .specialty(provider != null ? provider.getSpecialty() : "")
            .slotId(appt.getSlotId())
            .startTime(slot != null ? slot.getStartTime() : null)
            .endTime(slot != null ? slot.getEndTime() : null)
            .roomName(room != null ? room.getName() : "Unassigned")
            .status(appt.getStatus())
            .build();
    }
}
