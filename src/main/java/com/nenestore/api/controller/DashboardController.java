package com.nenestore.api.controller;

import com.nenestore.api.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return dashboardService.getStats();
    }

    @GetMapping("/orders")
    public List<Map<String, Object>> getLastOrders(
            @RequestParam(defaultValue = "10") int limit) {
        return dashboardService.getLastOrders(limit);
    }
}