package com.nenestore.api.service;

import com.nenestore.api.repository.ItemRepository;
import com.nenestore.api.repository.OrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;

    public DashboardService(ItemRepository itemRepository,
            OrderRepository orderRepository) {
        this.itemRepository = itemRepository;
        this.orderRepository = orderRepository;
    }

    public Map<String, Object> getStats() {
        Long totalStock = itemRepository.countStock();
        Long totalSold = itemRepository.countSold();
        BigDecimal totalPurchaseValue = itemRepository.sumPurchaseValue();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStock", totalStock != null ? totalStock : 0);
        stats.put("totalSold", totalSold != null ? totalSold : 0);
        stats.put("totalItems", (totalStock != null ? totalStock : 0)
                + (totalSold != null ? totalSold : 0));
        stats.put("totalPurchaseValueUsd",
                totalPurchaseValue != null ? totalPurchaseValue : BigDecimal.ZERO);
        return stats;
    }

    public List<Map<String, Object>> getLastOrders(int limit) {
        return orderRepository.findLastOrders(PageRequest.of(0, limit))
                .stream()
                .map(order -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("orderNumber", order.getOrderId());
                    map.put("orderDate", order.getOrderDate());
                    map.put("totalPrice", order.getTotalPrice());
                    map.put("totalItems", order.getTotalItems());
                    return map;
                })
                .collect(Collectors.toList());
    }
}