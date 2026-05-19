package com.nenestore.api.dto;

public class SheetOrder {
    private String orderNumber;
    private String orderDate;
    private Double totalPrice;
    private Integer totalItems;

    public SheetOrder(String orderNumber, String orderDate, Double totalPrice, Integer totalItems) {
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.totalPrice = totalPrice;
        this.totalItems = totalItems;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public Integer getTotalItems() {
        return totalItems;
    }
}