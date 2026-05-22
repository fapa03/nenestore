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
}