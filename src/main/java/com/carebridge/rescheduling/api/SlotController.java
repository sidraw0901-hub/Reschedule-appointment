package com.carebridge.rescheduling.api;

import com.carebridge.rescheduling.domain.model.Slot;
import com.carebridge.rescheduling.domain.model.SlotStatus;
import com.carebridge.rescheduling.domain.repository.SlotRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
@CrossOrigin(origins = "*")
public class SlotController {

    private final SlotRepository slotRepository;

    public SlotController(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @GetMapping("/available")
    public ResponseEntity<List<Slot>> getAvailableSlots() {
        return ResponseEntity.ok(slotRepository.findByStatusAndStartTimeAfterOrderByStartTimeAsc(SlotStatus.AVAILABLE, Instant.now()));
    }

    @GetMapping
    public ResponseEntity<List<Slot>> getAllSlots() {
        return ResponseEntity.ok(slotRepository.findAll());
    }
}
