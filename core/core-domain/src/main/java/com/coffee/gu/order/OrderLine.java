package com.coffee.gu.order;

import java.math.BigDecimal;

public class OrderLine {
    private Long id;
    private String orderKey;
    private Long menuId;
    private String menuName;
    private String imageUrl;
    private String description;
    private Long quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Boolean isStampEligible;

    public OrderLine(Long id, String orderKey, Long menuId, String menuName, String imageUrl, String description, Long quantity, BigDecimal unitPrice, BigDecimal totalPrice, Boolean isStampEligible) {
        this.id = id;
        this.orderKey = orderKey;
        this.menuId = menuId;
        this.menuName = menuName;
        this.imageUrl = imageUrl;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.isStampEligible = isStampEligible;
    }

    public static OrderLine create(String orderKey, Long menuId, String menuName, String imageUrl, String description, Long quantity, BigDecimal unitPrice, BigDecimal totalPrice, Boolean isStampEligible) {
        return new OrderLine(null,
                orderKey, menuId, menuName, imageUrl, description, quantity, unitPrice, totalPrice, isStampEligible);
    }

    public Long getId() {
        return id;
    }

    public Long getMenuId() {
        return menuId;
    }

    public String getMenuName() {
        return menuName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Long getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public String getDescription() {
        return description;
    }

    public String getOrderKey() {
        return orderKey;
    }

    public Boolean getIsStampEligible() {
        return isStampEligible;
    }
}
