package com.nenestore.api.dto;

public class SheetItem {
    private String orderId;
    private String product;
    private String imageUrl;
    private Integer quantity;
    private String color;
    private String size;
    private Double purchasePriceUsd;

    public SheetItem(String orderId, String product, String imageUrl,
            Integer quantity, String color, String size,
            Double purchasePriceUsd) {
        this.orderId = orderId;
        this.product = product;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.color = color;
        this.size = size;
        this.purchasePriceUsd = purchasePriceUsd;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getProduct() {
        return product;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getColor() {
        return color;
    }

    public String getSize() {
        return size;
    }

    public Double getPurchasePriceUsd() {
        return purchasePriceUsd;
    }
}