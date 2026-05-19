package com.nenestore.api.controller;

import com.nenestore.api.service.GoogleSheetsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final GoogleSheetsService googleSheetsService;

    public HealthController(GoogleSheetsService googleSheetsService) {
        this.googleSheetsService = googleSheetsService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "service", "nenestore-api");
    }

    @GetMapping("/sheets-test")
    public Object sheetsTest() {
        try {
            return googleSheetsService.getOrders();
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}