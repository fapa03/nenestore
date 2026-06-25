package com.nenestore.api.service;

import com.nenestore.api.dto.SheetItem;
import com.nenestore.api.dto.SheetOrder;
import com.nenestore.api.entity.Item;
import com.nenestore.api.entity.Order;
import com.nenestore.api.entity.SyncLog;
import com.nenestore.api.repository.ItemRepository;
import com.nenestore.api.repository.OrderRepository;
import com.nenestore.api.repository.SyncLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nenestore.api.service.GenderTagger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SyncService {

    private final GoogleSheetsService googleSheetsService;
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final SyncLogRepository syncLogRepository;
    private final SkuService skuService;
    private final ImageService imageService;
    private final GenderTagger genderTagger;

    public SyncService(GoogleSheetsService googleSheetsService,
            OrderRepository orderRepository,
            ItemRepository itemRepository,
            SyncLogRepository syncLogRepository,
            SkuService skuService,
            ImageService imageService,
            GenderTagger genderTagger) {
        this.googleSheetsService = googleSheetsService;
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.syncLogRepository = syncLogRepository;
        this.skuService = skuService;
        this.imageService = imageService;
        this.genderTagger = genderTagger;
    }

    // ── STEP 1+2: Read sheets and filter new orders ──────────────────────────
    public List<Map<String, Object>> previewNewOrders() throws Exception {
        List<SheetOrder> allOrders = googleSheetsService.getOrders();
        List<SheetItem> allItems = googleSheetsService.getItems();

        List<Map<String, Object>> preview = new ArrayList<>();

        for (SheetOrder sheetOrder : allOrders) {
            // STEP 2 — skip already imported orders
            if (orderRepository.findByOrderId(sheetOrder.getOrderNumber()).isPresent()) {
                continue;
            }

            // collect items for this order
            List<Map<String, Object>> itemPreviews = new ArrayList<>();
            for (SheetItem item : allItems) {
                if (!item.getOrderId().equals(sheetOrder.getOrderNumber()))
                    continue;

                // STEP 4 — auto tag gender
                String gender = genderTagger.tag(
                        sheetOrder.getOrderNumber(),
                        item.getProduct(),
                        item.getSize());

                Map<String, Object> itemMap = new LinkedHashMap<>();
                itemMap.put("product", item.getProduct());
                itemMap.put("color", item.getColor());
                itemMap.put("size", item.getSize());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("purchasePriceUsd", item.getPurchasePriceUsd());
                itemMap.put("imageUrl", item.getImageUrl());
                itemMap.put("gender", gender);
                itemPreviews.add(itemMap);
            }

            Map<String, Object> orderMap = new LinkedHashMap<>();
            orderMap.put("orderNumber", sheetOrder.getOrderNumber());
            orderMap.put("orderDate", sheetOrder.getOrderDate());
            orderMap.put("totalPrice", sheetOrder.getTotalPrice());
            orderMap.put("items", itemPreviews);
            preview.add(orderMap);
        }

        return preview;
    }

    // ── STEP 5-9: Process and insert confirmed orders ─────────────────────────
    @Transactional
    public Map<String, Object> confirmSync(List<Map<String, Object>> confirmedOrders) {
        SyncLog log = new SyncLog();
        log.setStartedAt(LocalDateTime.now());
        log.setStatus("RUNNING");
        syncLogRepository.save(log);

        int totalProcessed = 0;

        try {
            for (Map<String, Object> orderData : confirmedOrders) {
                String orderNumber = (String) orderData.get("orderNumber");

                // double check — skip if already exists
                if (orderRepository.findByOrderId(orderNumber).isPresent())
                    continue;

                // STEP 3 — parse and validate date
                LocalDate orderDate = parseDate((String) orderData.get("orderDate"));

                // build and save order
                Order order = new Order();
                order.setOrderId(orderNumber);
                order.setOrderDate(orderDate);
                order.setTotalPrice(BigDecimal.valueOf(
                        ((Number) orderData.get("totalPrice")).doubleValue()));
                order.setTotalItems(0);
                order.setCreatedAt(LocalDateTime.now());
                order.setUpdatedAt(LocalDateTime.now());
                Order savedOrder = orderRepository.save(order);

                // STEP 5+6+7+8 — expand items into units
                List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");

                int unitIndex = skuService.getNextUnitIndex(orderNumber);
                int orderUnitCount = 0;
                for (Map<String, Object> itemData : items) {
                    int quantity = ((Number) itemData.get("quantity")).intValue();
                    String gender = (String) itemData.get("gender");
                    String product = (String) itemData.get("product");
                    String color = (String) itemData.get("color");
                    String size = (String) itemData.get("size");
                    String imageUrl = (String) itemData.get("imageUrl");
                    BigDecimal price = BigDecimal.valueOf(
                            ((Number) itemData.get("purchasePriceUsd")).doubleValue());

                    // STEP 5 — expand by quantity
                    for (int i = 0; i < quantity; i++) {
                        // STEP 6 — generate SKU
                        String sku = skuService.generateSku(orderNumber, unitIndex);

                        // STEP 7 — download image
                        String localImagePath = imageService.downloadImage(imageUrl, sku);

                        // STEP 8 — build and save item
                        Item item = new Item();
                        item.setOrder(savedOrder);
                        item.setSku(sku);
                        item.setProduct(product);
                        item.setColor(color);
                        item.setSize(size);
                        item.setGender(gender);
                        item.setPurchasePriceUsd(price);
                        item.setImageUrl(localImagePath);
                        item.setStatus("Stock");
                        item.setCreatedAt(LocalDateTime.now());
                        item.setUpdatedAt(LocalDateTime.now());
                        itemRepository.save(item);
                        orderUnitCount++;
                        unitIndex++;
                        totalProcessed++;
                    }
                }

                // update total_items with real unit count
                savedOrder.setTotalItems(orderUnitCount);
                orderRepository.save(savedOrder);
            }

            // STEP 9 — log success
            log.setFinishedAt(LocalDateTime.now());
            log.setRowsProcessed(totalProcessed);
            log.setStatus("SUCCESS");
            syncLogRepository.save(log);

            return Map.of(
                    "status", "SUCCESS",
                    "itemsProcessed", totalProcessed);

        } catch (Exception e) {
            log.setFinishedAt(LocalDateTime.now());
            log.setStatus("FAILED");
            log.setErrorMsg(e.getMessage());
            syncLogRepository.save(log);
            throw new RuntimeException("Sync failed: " + e.getMessage(), e);
        }
    }

    // ── Date parser with fallback ─────────────────────────────────────────────
    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception ignored) {
        }

        try {
            DateTimeFormatter fallback = DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss");
            return LocalDate.parse(raw.trim(), fallback);
        } catch (Exception ignored) {
        }

        throw new RuntimeException("Unparseable date: " + raw);
    }

    public Map<String, Object> syncAll() throws Exception {
        List<Map<String, Object>> newOrders = previewNewOrders();
        if (newOrders.isEmpty()) {
            return Map.of("status", "UP_TO_DATE", "itemsProcessed", 0);
        }
        return confirmSync(newOrders);
    }

}