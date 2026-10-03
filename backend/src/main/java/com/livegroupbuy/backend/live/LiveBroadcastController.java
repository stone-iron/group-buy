package com.livegroupbuy.backend.live;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/live-broadcasts")
public class LiveBroadcastController {

    private final LiveBroadcastService service;

    public LiveBroadcastController(LiveBroadcastService service) {
        this.service = service;
    }

    @GetMapping
    public List<LiveBroadcastResponse> findAll() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<LiveBroadcastResponse> create(@Valid @RequestBody LiveBroadcastRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PostMapping("/import")
    public List<LiveBroadcastResponse> importLegacy(@Valid @RequestBody List<LiveBroadcastRequest> requests) {
        return service.importLegacy(requests);
    }

    @PatchMapping("/{id}/status")
    public LiveBroadcastResponse updateStatus(@PathVariable Long id, @Valid @RequestBody LiveStatusRequest request) {
        return service.updateStatus(id, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
