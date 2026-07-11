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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.nenestore.api.service.GenderTagger;
import com.nenestore.api.exception.BadRequestException;

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
    private final SizeNormalizer sizeNormalizer;

    public SyncService(GoogleSheetsService googleSheetsService,
            OrderRepository orderRepository,
            ItemRepository itemRepository,
            SyncLogRepository syncLogRepository,
            SkuService skuService,
            ImageService imageService,
            GenderTagger genderTagger,
            SizeNormalizer sizeNormalizer) {
        this.googleSheetsService = googleSheetsService;
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.syncLogRepository = syncLogRepository;
        this.skuService = skuService;
        this.imageService = imageService;
        this.genderTagger = genderTagger;
        this.sizeNormalizer = sizeNormalizer;
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

        throw new BadRequestException("Unparseable date: " + raw);
    }

    public Map<String, Object> syncAll() throws Exception {
        List<Map<String, Object>> newOrders = previewNewOrders();
        if (newOrders.isEmpty()) {
            return Map.of("status", "UP_TO_DATE", "itemsProcessed", 0);
        }
        return confirmSync(newOrders);
    }

    public Map<String, Object> repairMissingImages() throws Exception {
        // get all items with image_url but missing file on disk
        List<Item> allItems = itemRepository.findAll();
        List<Item> missingImages = allItems.stream()
                .filter(i -> i.getImageUrl() != null)
                .filter(i -> !imageService.imageExistsOnDisk(i.getImageUrl()))
                .toList();

        System.out.println("Items missing images: " + missingImages.size());

        // get all sheet items to find original Shopify URLs
        List<SheetItem> sheetItems = googleSheetsService.getItems();

        int repaired = 0;
        int failed = 0;

        for (Item item : missingImages) {
            // find matching sheet row by order_id + product name
            String orderId = item.getOrder().getOrderId();
            String product = item.getProduct();

            SheetItem match = sheetItems.stream()
                    .filter(si -> si.getOrderId().equals(orderId)
                            && si.getProduct().equals(product))
                    .findFirst()
                    .orElse(null);

            if (match == null || match.getImageUrl() == null
                    || match.getImageUrl().isBlank()) {
                System.out.println("No sheet match for: " + item.getSku());
                failed++;
                continue;
            }

            // re-download the image
            String localPath = imageService.downloadImage(
                    match.getImageUrl(), item.getSku());

            if (localPath != null) {
                System.out.println("Repaired: " + item.getSku());
                repaired++;
            } else {
                System.out.println("Download failed: " + item.getSku());
                failed++;
            }
        }

        return Map.of(
                "missing", missingImages.size(),
                "repaired", repaired,
                "failed", failed);
    }

    private void emit(SseEmitter emitter, String type, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(type)
                    .data(data));
        } catch (Exception e) {
            // client disconnected
        }
    }

    public void syncWithProgress(SseEmitter emitter) {
        try {
            // STEP 1 — connect to Sheets
            emit(emitter, "step", Map.of(
                    "step", 1,
                    "status", "running",
                    "message", "Connecting to Google Sheets..."));

            List<SheetOrder> allOrders = googleSheetsService.getOrders();
            List<SheetItem> allItems = googleSheetsService.getItems();

            emit(emitter, "step", Map.of(
                    "step", 1,
                    "status", "done",
                    "message", "Connected to Google Sheets"));

            // STEP 2 — scan for new orders
            emit(emitter, "step", Map.of(
                    "step", 2,
                    "status", "running",
                    "message", "Scanning for new orders..."));

            List<SheetOrder> newOrders = allOrders.stream()
                    .filter(o -> orderRepository.findByOrderId(o.getOrderNumber()).isEmpty())
                    .toList();

            if (newOrders.isEmpty()) {
                emit(emitter, "step", Map.of(
                        "step", 2,
                        "status", "done",
                        "message", "No new orders found — already up to date"));
                emit(emitter, "complete", Map.of(
                        "status", "UP_TO_DATE",
                        "itemsProcessed", 0));
                emitter.complete();
                return;
            }

            emit(emitter, "step", Map.of(
                    "step", 2,
                    "status", "done",
                    "message", "Detected " + newOrders.size() + " new order(s)"));

            int totalProcessed = 0;

            // STEP 3 — process each order
            for (SheetOrder sheetOrder : newOrders) {
                emit(emitter, "step", Map.of(
                        "step", 3,
                        "status", "running",
                        "message", "Processing order: " + sheetOrder.getOrderNumber(),
                        "detail", "$" + sheetOrder.getTotalPrice() + " USD"));

                // collect items for this order
                List<SheetItem> orderItems = allItems.stream()
                        .filter(i -> i.getOrderId().equals(sheetOrder.getOrderNumber()))
                        .toList();

                // tag gender
                List<Map<String, Object>> taggedItems = new ArrayList<>();
                for (SheetItem si : orderItems) {
                    String gender = genderTagger.tag(
                            sheetOrder.getOrderNumber(), si.getProduct(), si.getSize());
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("product", si.getProduct());
                    itemMap.put("color", si.getColor());
                    itemMap.put("size", si.getSize());
                    itemMap.put("quantity", si.getQuantity());
                    itemMap.put("purchasePriceUsd", si.getPurchasePriceUsd());
                    itemMap.put("imageUrl", si.getImageUrl());
                    itemMap.put("gender", gender);
                    taggedItems.add(itemMap);
                }

                // STEP 4 — download images
                emit(emitter, "step", Map.of(
                        "step", 4,
                        "status", "running",
                        "message", "Downloading images for " + sheetOrder.getOrderNumber() + "..."));

                // STEP 5 — insert to DB
                emit(emitter, "step", Map.of(
                        "step", 5,
                        "status", "running",
                        "message", "Inserting records into database..."));

                // build confirm payload and process
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("orderNumber", sheetOrder.getOrderNumber());
                orderData.put("orderDate", sheetOrder.getOrderDate());
                orderData.put("totalPrice", sheetOrder.getTotalPrice());
                orderData.put("items", taggedItems);

                Map<String, Object> result = confirmSync(List.of(orderData));
                int processed = ((Number) result.get("itemsProcessed")).intValue();
                totalProcessed += processed;

                emit(emitter, "step", Map.of(
                        "step", 5,
                        "status", "done",
                        "message", "Order " + sheetOrder.getOrderNumber() + " inserted",
                        "detail", processed + " units"));
            }

            // STEP 6 — complete
            emit(emitter, "complete", Map.of(
                    "status", "SUCCESS",
                    "itemsProcessed", totalProcessed));

            emitter.complete();

        } catch (Exception e) {
            emit(emitter, "error", Map.of(
                    "message", "Sync failed: " + e.getMessage()));
            emitter.completeWithError(e);
        }
    }

    @Transactional
    public Map<String, Object> refreshDatabase() throws Exception {
        List<Map<String, String>> sheetRows = googleSheetsService.getItemPriceUpdates();

        int matched = 0;
        int updated = 0;
        int skipped = 0;
        int notFound = 0;

        for (Map<String, String> row : sheetRows) {
            String orderId = row.get("orderId");
            String product = row.get("product");
            String color = row.get("color");
            String sheetSize = sizeNormalizer.normalize(row.get("size"));
            String stock = row.get("stock");
            String priceStr = row.get("salePriceMxn");
            String purchaseStr = row.get("purchasePriceUsd");

            if (orderId.isBlank() || product.isBlank()) {
                skipped++;
                continue;
            }

            // find candidates by order + product + color
            List<Item> candidates = itemRepository
                    .findByOrderProductColor(orderId, product, color);

            if (candidates.isEmpty()) {
                notFound++;
                continue;
            }

            // filter by normalized size
            List<Item> matches = candidates.stream()
                    .filter(i -> sizeNormalizer.normalize(i.getSize()).equals(sheetSize))
                    .toList();

            if (matches.isEmpty()) {
                notFound++;
                continue;
            }

            matched += matches.size();

            for (Item item : matches) {
                boolean changed = false;

                // status
                if (stock != null && !stock.isBlank()) {
                    String newStatus = stock.equalsIgnoreCase("Vendido")
                            ? "Unavailable"
                            : "Stock";
                    if (!newStatus.equals(item.getStatus())) {
                        item.setStatus(newStatus);
                        changed = true;
                    }
                }

                // sale price
                if (priceStr != null && !priceStr.isBlank()) {
                    try {
                        BigDecimal price = new BigDecimal(priceStr);
                        if (item.getSalePriceMxn() == null ||
                                price.compareTo(item.getSalePriceMxn()) != 0) {
                            item.setSalePriceMxn(price);
                            changed = true;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }

                // purchase price
                if (purchaseStr != null && !purchaseStr.isBlank()) {
                    try {
                        BigDecimal purchase = new BigDecimal(purchaseStr);
                        if (purchase.compareTo(item.getPurchasePriceUsd()) != 0) {
                            item.setPurchasePriceUsd(purchase);
                            changed = true;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }

                if (changed) {
                    item.setUpdatedAt(LocalDateTime.now());
                    itemRepository.save(item);
                    updated++;
                } else {
                    skipped++;
                }
            }
        }

        return Map.of(
                "rowsRead", sheetRows.size(),
                "matched", matched,
                "updated", updated,
                "skipped", skipped,
                "notFound", notFound);
    }

}