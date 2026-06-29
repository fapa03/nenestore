package com.nenestore.api.controller;

import com.nenestore.api.service.SyncService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping("/preview")
    public List<Map<String, Object>> preview() throws Exception {
        return syncService.previewNewOrders();
    }

    @PostMapping("/confirm")
    public Map<String, Object> confirm(@RequestBody List<Map<String, Object>> confirmedOrders) throws Exception {
        return syncService.confirmSync(confirmedOrders);
    }

    @PostMapping("/all")
    public Map<String, Object> syncAll() throws Exception {
        return syncService.syncAll();
    }

    @PostMapping("/repair-images")
    public Map<String, Object> repairImages() throws Exception {
        return syncService.repairMissingImages();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter syncStream() {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min timeout
        new Thread(() -> syncService.syncWithProgress(emitter)).start();
        return emitter;
    }

}