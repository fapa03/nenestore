package com.nenestore.api.controller;

import com.nenestore.api.dto.InventoryResponse;
import com.nenestore.api.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryResponse> getInventory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String size) {
        return inventoryService.getInventory(search, status, gender, size);
    }

    @PatchMapping("/{id}/status")
    public InventoryResponse updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return inventoryService.updateStatus(id, body.get("status"));
    }

    @PatchMapping("/{id}/price")
    public InventoryResponse updatePrice(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return inventoryService.updatePrice(id, new BigDecimal(body.get("price")));
    }
}