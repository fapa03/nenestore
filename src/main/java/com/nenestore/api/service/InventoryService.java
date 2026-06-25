package com.nenestore.api.service;

import com.nenestore.api.dto.InventoryResponse;
import com.nenestore.api.entity.Item;
import com.nenestore.api.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final ItemRepository itemRepository;

    public InventoryService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public List<InventoryResponse> getInventory(String search, String status,
            String gender, String size) {
        // convert empty strings to null so the query skips those filters
        String s = (search != null && !search.isBlank()) ? search : null;
        String st = (status != null && !status.isBlank()) ? status : null;
        String g = (gender != null && !gender.isBlank()) ? gender : null;
        String sz = (size != null && !size.isBlank()) ? size : null;

        List<Item> items = itemRepository.searchInventory(s, st, g, sz);
        return items.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public InventoryResponse updateStatus(Long id, String status) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found: " + id));
        item.setStatus(status);
        item.setUpdatedAt(java.time.LocalDateTime.now());
        return toResponse(itemRepository.save(item));
    }

    public InventoryResponse updatePrice(Long id, java.math.BigDecimal price) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found: " + id));
        item.setSalePriceMxn(price);
        item.setUpdatedAt(java.time.LocalDateTime.now());
        return toResponse(itemRepository.save(item));
    }

    private InventoryResponse toResponse(Item item) {
        return new InventoryResponse(
                item.getId(),
                item.getSku(),
                item.getProduct(),
                item.getColor(),
                item.getSize(),
                item.getGender(),
                item.getStatus(),
                item.getPurchasePriceUsd(),
                item.getSalePriceMxn(),
                item.getImageUrl(),
                item.getBarcode(),
                item.getOrder().getOrderId(),
                item.getOrder().getOrderDate());
    }
}