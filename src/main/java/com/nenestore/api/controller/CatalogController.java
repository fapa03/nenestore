package com.nenestore.api.controller;

import com.nenestore.api.service.CatalogService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> generateCatalog(
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String search) throws Exception {

        byte[] pdf = catalogService.generateCatalog(gender, status, size, search);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"nenestore-catalog.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}