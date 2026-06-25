package com.nenestore.api.controller;

import com.nenestore.api.service.SaleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    public Map<String, Object> registerSale(@RequestBody Map<String, Object> body) {
        return saleService.registerSale(body);
    }

    @GetMapping
    public List<Map<String, Object>> getAllSales() {
        return saleService.getAllSales();
    }

    @GetMapping("/credit")
    public List<Map<String, Object>> getOpenCreditSales() {
        return saleService.getOpenCreditSales();
    }

    @PostMapping("/credit/{creditSaleId}/payments")
    public Map<String, Object> registerPayment(
            @PathVariable Long creditSaleId,
            @RequestBody Map<String, Object> body) {
        return saleService.registerPayment(creditSaleId, body);
    }
}