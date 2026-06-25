package com.nenestore.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InventoryResponse {

    private Long id;
    private String sku;
    private String product;
    private String color;
    private String size;
    private String gender;
    private String status;
    private BigDecimal purchasePriceUsd;
    private BigDecimal salePriceMxn;
    private String imageUrl;
    private String barcode;
    private String orderNumber;
    private LocalDate orderDate;

    public InventoryResponse(Long id, String sku, String product, String color,
            String size, String gender, String status,
            BigDecimal purchasePriceUsd, BigDecimal salePriceMxn,
            String imageUrl, String barcode,
            String orderNumber, LocalDate orderDate) {
        this.id = id;
        this.sku = sku;
        this.product = product;
        this.color = color;
        this.size = size;
        this.gender = gender;
        this.status = status;
        this.purchasePriceUsd = purchasePriceUsd;
        this.salePriceMxn = salePriceMxn;
        this.imageUrl = imageUrl;
        this.barcode = barcode;
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getProduct() {
        return product;
    }

    public String getColor() {
        return color;
    }

    public String getSize() {
        return size;
    }

    public String getGender() {
        return gender;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getPurchasePriceUsd() {
        return purchasePriceUsd;
    }

    public BigDecimal getSalePriceMxn() {
        return salePriceMxn;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }
}