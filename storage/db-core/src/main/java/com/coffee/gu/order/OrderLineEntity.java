package com.coffee.gu.order;

import com.coffee.gu.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "order_line")
public class OrderLineEntity extends BaseEntity {
    private String orderKey;
    private Long menuId;
    private String menuName;
    private String imageUrl;
    private String description;
    private Long quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Boolean isStampEligible;

    public OrderLineEntity() {}

    public OrderLineEntity(String orderKey, Long menuId, String menuName, String imageUrl, String description, Long quantity, BigDecimal unitPrice, BigDecimal totalPrice, Boolean isStampEligible) {
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

    public static OrderLineEntity from(OrderLine orderLine) {
        return new OrderLineEntity(
                orderLine.getOrderKey(),
                orderLine.getMenuId(),
                orderLine.getMenuName(),
                orderLine.getImageUrl(),
                orderLine.getDescription(),
                orderLine.getQuantity(),
                orderLine.getUnitPrice(),
                orderLine.getTotalPrice(),
                orderLine.getIsStampEligible()
        );
    }

    public OrderLine toModel() {
        return new OrderLine(
                this.id,
                this.orderKey,
                this.menuId,
                this.menuName,
                this.imageUrl,
                this.description,
                this.quantity,
                this.unitPrice,
                this.totalPrice,
                this.isStampEligible
        );
    }

    public String getOrderKey() {
        return orderKey;
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
}
