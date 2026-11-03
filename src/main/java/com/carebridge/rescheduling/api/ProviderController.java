package com.carebridge.rescheduling.api;

import com.carebridge.rescheduling.domain.model.Appointment;
import com.carebridge.rescheduling.domain.model.AppointmentStatus;
import com.carebridge.rescheduling.domain.model.Provider;
import com.carebridge.rescheduling.domain.model.ProviderStatus;
import com.carebridge.rescheduling.domain.model.Room;
import com.carebridge.rescheduling.domain.model.Slot;
import com.carebridge.rescheduling.domain.model.SlotStatus;
import com.carebridge.rescheduling.domain.repository.AppointmentRepository;
import com.carebridge.rescheduling.domain.repository.ProviderRepository;
import com.carebridge.rescheduling.domain.repository.RoomRepository;
import com.carebridge.rescheduling.domain.repository.SlotRepository;
import com.carebridge.rescheduling.workflow.dto.ProviderBandwidthDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/providers")
@CrossOrigin(origins = "*")
public class ProviderController {

    private final ProviderRepository providerRepository;
    private final SlotRepository slotRepository;
    private final RoomRepository roomRepository;
    private final AppointmentRepository appointmentRepository;

    public ProviderController(ProviderRepository providerRepository,
                              SlotRepository slotRepository,
                              RoomRepository roomRepository,
                              AppointmentRepository appointmentRepository) {
        this.providerRepository = providerRepository;
        this.slotRepository = slotRepository;
        this.roomRepository = roomRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping
    public ResponseEntity<List<Provider>> getProviders() {
        return ResponseEntity.ok(providerRepository.findAll());
    }

    @GetMapping("/bandwidth")
    public ResponseEntity<List<ProviderBandwidthDTO>> getProviderBandwidths() {
        List<Provider> providers = providerRepository.findAll();
        List<Slot> allSlots = slotRepository.findAll();
        List<Appointment> allAppts = appointmentRepository.findAll();

        Map<UUID, Room> roomMap = roomRepository.findAll().stream()
            .collect(Collectors.toMap(Room::getId, r -> r));

        List<ProviderBandwidthDTO> results = new ArrayList<>();

        for (Provider provider : providers) {
            // Count active booked appointments currently assigned to this doctor
            long activeBooked = allAppts.stream()
                .filter(a -> a.getProviderId().equals(provider.getId()) &&
                            (a.getStatus() == AppointmentStatus.BOOKED || a.getStatus() == AppointmentStatus.RESCHEDULED))
                .count();

            int totalSlots = 4; // Every doctor has exactly 4 slots total capacity invariant
            int bookedSlots = (int) Math.min(totalSlots, activeBooked);
            
            // If offline, available bandwidth is 0. If available, remaining slots up to 4
            int availableBandwidth = provider.getStatus() == ProviderStatus.OFFLINE 
                ? 0 
                : Math.max(0, totalSlots - bookedSlots);

            int utilPercent = (int) Math.round(((double) bookedSlots / totalSlots) * 100.0);

            // Find assigned room name
            String roomName = "Exam Room";
            List<Slot> providerSlots = allSlots.stream()
                .filter(s -> s.getProviderId().equals(provider.getId()))
                .collect(Collectors.toList());

            if (!providerSlots.isEmpty()) {
                Room r = roomMap.get(providerSlots.get(0).getRoomId());
                if (r != null) {
                    roomName = r.getName();
                }
            }

            results.add(new ProviderBandwidthDTO(
                provider.getId(),
                provider.getName(),
                provider.getSpecialty(),
                provider.getStatus(),
                totalSlots,
                bookedSlots,
                availableBandwidth,
                utilPercent,
                roomName
            ));
        }

        return ResponseEntity.ok(results);
    }
}
