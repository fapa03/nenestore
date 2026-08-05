package com.nenestore.api.service;

import com.nenestore.api.entity.*;
import com.nenestore.api.exception.ConflictException;
import com.nenestore.api.exception.ResourceNotFoundException;
import com.nenestore.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final CreditSaleRepository creditSaleRepository;
    private final PaymentRepository paymentRepository;
    private final ItemRepository itemRepository;
    private final ClientRepository clientRepository;

    public SaleService(SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            CreditSaleRepository creditSaleRepository,
            PaymentRepository paymentRepository,
            ItemRepository itemRepository,
            ClientRepository clientRepository) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.creditSaleRepository = creditSaleRepository;
        this.paymentRepository = paymentRepository;
        this.itemRepository = itemRepository;
        this.clientRepository = clientRepository;
    }

    // ── Register a new sale ───────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> registerSale(Map<String, Object> body) {

        // extract fields
        Long clientId = body.get("clientId") != null
                ? ((Number) body.get("clientId")).longValue()
                : null;
        String paymentType = (String) body.getOrDefault("paymentType", "CASH");
        BigDecimal discountMxn = new BigDecimal(
                body.getOrDefault("discountMxn", "0").toString());
        String notes = (String) body.get("notes");
        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) body.get("items");

        // build sale
        Sale sale = new Sale();
        sale.setSaleDate(LocalDate.now());
        sale.setPaymentType(paymentType);
        sale.setDiscountMxn(discountMxn);
        sale.setNotes(notes);
        sale.setCreatedAt(LocalDateTime.now());
        sale.setUpdatedAt(LocalDateTime.now());

        // attach client if provided
        if (clientId != null) {
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));
            sale.setClient(client);
        }

        Sale savedSale = saleRepository.save(sale);

        // process items
        BigDecimal total = BigDecimal.ZERO;
        List<Map<String, Object>> processedItems = new ArrayList<>();

        for (Map<String, Object> itemData : itemsData) {
            Long itemId = ((Number) itemData.get("itemId")).longValue();
            BigDecimal priceMxn = new BigDecimal(itemData.get("priceMxn").toString());

            Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemId));

            if (!item.getStatus().equals("Stock")) {
                throw new ConflictException("Item " + item.getSku() + " is not available");
            }

            // mark item as sold
            item.setStatus("Unavailable");
            item.setSelledPriceMxn(priceMxn);
            item.setUpdatedAt(LocalDateTime.now());
            itemRepository.save(item);

            // create sale item record
            SaleItem saleItem = new SaleItem();
            saleItem.setSale(savedSale);
            saleItem.setItem(item);
            saleItem.setPriceMxn(priceMxn);
            saleItemRepository.save(saleItem);

            total = total.add(priceMxn);

            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("sku", item.getSku());
            itemMap.put("product", item.getProduct());
            itemMap.put("priceMxn", priceMxn);
            processedItems.add(itemMap);
        }

        // apply discount
        BigDecimal finalTotal = total.subtract(discountMxn);
        savedSale.setTotalMxn(finalTotal);
        saleRepository.save(savedSale);

        // if credit — create credit sale record
        if ("CREDIT".equalsIgnoreCase(paymentType)) {
            CreditSale creditSale = new CreditSale();
            creditSale.setSale(savedSale);
            creditSale.setOriginalDebt(finalTotal);
            creditSale.setPaidAmount(BigDecimal.ZERO);
            creditSale.setBalance(finalTotal);
            creditSale.setStatus("PENDING");
            creditSale.setCreatedAt(LocalDateTime.now());
            creditSale.setUpdatedAt(LocalDateTime.now());
            creditSaleRepository.save(creditSale);
        }

        // build response
        Map<String, Object> response = new HashMap<>();
        response.put("saleId", savedSale.getId());
        response.put("paymentType", paymentType);
        response.put("subtotal", total);
        response.put("discountMxn", discountMxn);
        response.put("totalMxn", finalTotal);
        response.put("items", processedItems);
        response.put("status", "SUCCESS");
        return response;
    }

    // ── Register a payment against a credit sale ──────────────────────────────
    @Transactional
    public Map<String, Object> registerPayment(Long creditSaleId,
            Map<String, Object> body) {
        CreditSale creditSale = creditSaleRepository.findById(creditSaleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Credit sale not found: " + creditSaleId));

        if (creditSale.getStatus().equals("PAID")) {
            throw new ConflictException("This credit sale is already fully paid");
        }

        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String notes = (String) body.get("notes");

        // insert payment record
        Payment payment = new Payment();
        payment.setCreditSale(creditSale);
        payment.setAmount(amount);
        payment.setPaymentDate(LocalDate.now());
        payment.setNotes(notes);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // recalculate balance
        BigDecimal totalPaid = paymentRepository
                .sumPaymentsByCreditSaleId(creditSaleId);
        BigDecimal newBalance = creditSale.getOriginalDebt().subtract(totalPaid);

        creditSale.setPaidAmount(totalPaid);
        creditSale.setBalance(newBalance.max(BigDecimal.ZERO));
        creditSale.setStatus(
                newBalance.compareTo(BigDecimal.ZERO) <= 0 ? "PAID"
                        : totalPaid.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "PENDING");
        creditSale.setUpdatedAt(LocalDateTime.now());
        creditSaleRepository.save(creditSale);

        Map<String, Object> response = new HashMap<>();
        response.put("creditSaleId", creditSaleId);
        response.put("amountPaid", amount);
        response.put("totalPaid", totalPaid);
        response.put("balance", creditSale.getBalance());
        response.put("status", creditSale.getStatus());
        return response;
    }

    // ── List all sales ────────────────────────────────────────────────────────
    public List<Map<String, Object>> getAllSales() {
        return saleRepository.findAllWithDetails().stream().map(sale -> {
            Map<String, Object> map = new HashMap<>();
            map.put("saleId", sale.getId());
            map.put("saleDate", sale.getSaleDate());
            map.put("paymentType", sale.getPaymentType());
            map.put("discountMxn", sale.getDiscountMxn());
            map.put("totalMxn", sale.getTotalMxn());
            map.put("notes", sale.getNotes());
            map.put("client", sale.getClient() != null ? Map.of(
                    "id", sale.getClient().getId(),
                    "name", sale.getClient().getName(),
                    "whatsapp", sale.getClient().getWhatsapp() != null
                            ? sale.getClient().getWhatsapp()
                            : "")
                    : null);
            map.put("items", sale.getSaleItems().stream().map(si -> Map.of(
                    "sku", si.getItem().getSku(),
                    "product", si.getItem().getProduct(),
                    "priceMxn", si.getPriceMxn())).toList());
            return map;
        }).toList();
    }

    // ── List open credit sales ────────────────────────────────────────────────
    public List<Map<String, Object>> getOpenCreditSales() {
        return creditSaleRepository.findAllOpen().stream().map(cs -> {
            Map<String, Object> map = new HashMap<>();
            map.put("creditSaleId", cs.getId());
            map.put("saleId", cs.getSale().getId());
            map.put("saleDate", cs.getSale().getSaleDate());
            map.put("originalDebt", cs.getOriginalDebt());
            map.put("paidAmount", cs.getPaidAmount());
            map.put("balance", cs.getBalance());
            map.put("status", cs.getStatus());
            map.put("client", cs.getSale().getClient() != null ? Map.of(
                    "id", cs.getSale().getClient().getId(),
                    "name", cs.getSale().getClient().getName(),
                    "whatsapp", cs.getSale().getClient().getWhatsapp() != null
                            ? cs.getSale().getClient().getWhatsapp()
                            : "")
                    : null);
            // ← add payment history
            List<Map<String, Object>> payments = cs.getPayments() != null
                    ? cs.getPayments().stream().map(p -> {
                        Map<String, Object> pm = new HashMap<>();
                        pm.put("date", p.getPaymentDate());
                        pm.put("amount", p.getAmount());
                        pm.put("notes", p.getNotes());
                        return pm;
                    }).toList()
                    : List.of();
            map.put("payments", payments);

            return map;
        }).toList();
    }
}