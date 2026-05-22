package com.nenestore.api.service;

import com.nenestore.api.repository.ItemRepository;
import org.springframework.stereotype.Service;

@Service
public class SkuService {

    private final ItemRepository itemRepository;

    public SkuService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public String generateSku(String orderId, int unitIndex) {
        return orderId + "-" + String.format("%03d", unitIndex);
    }

    public int getNextUnitIndex(String orderId) {
        Integer max = itemRepository.findMaxUnitIndexByOrderId(orderId);
        return (max == null) ? 1 : max + 1;
    }
}