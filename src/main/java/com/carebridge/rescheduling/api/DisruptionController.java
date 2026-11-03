package com.carebridge.rescheduling.api;

import com.carebridge.rescheduling.workflow.dto.CancelSlotRequest;
import com.carebridge.rescheduling.workflow.dto.DisruptionRequest;
import com.carebridge.rescheduling.workflow.service.DisruptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DisruptionController {

    private final DisruptionService disruptionService;

    public DisruptionController(DisruptionService disruptionService) {
        this.disruptionService = disruptionService;
    }

    @PostMapping("/providers/{id}/offline")
    public ResponseEntity<Map<String, Object>> markProviderOffline(
            @PathVariable UUID id,
            @RequestBody(required = false) DisruptionRequest request) {

        List<UUID> affectedAppointments = disruptionService.markProviderOffline(id, request);
        return ResponseEntity.accepted().body(Map.of(
            "message", "Provider offline disruption registered",
            "providerId", id,
            "affectedAppointmentCount", affectedAppointments.size(),
            "affectedAppointmentIds", affectedAppointments
        ));
    }

    @PostMapping("/providers/{id}/restore")
    public ResponseEntity<Map<String, Object>> restoreProvider(@PathVariable UUID id) {
        disruptionService.restoreProvider(id);
        return ResponseEntity.ok(Map.of(
            "message", "Provider status restored to AVAILABLE",
            "providerId", id
        ));
    }

    @PostMapping("/slots/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelSlot(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelSlotRequest request) {

        String reason = request != null ? request.getReason() : "Slot cancelled by administrator";
        disruptionService.cancelSlot(id, reason);
        return ResponseEntity.ok(Map.of(
            "message", "Slot cancelled successfully",
            "slotId", id
        ));
    }
}
