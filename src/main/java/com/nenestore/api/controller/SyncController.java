package com.nenestore.api.controller;

import com.nenestore.api.service.SyncService;
import org.springframework.web.bind.annotation.*;

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
}