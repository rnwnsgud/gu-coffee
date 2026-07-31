package com.coffee.gu.order;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.OrderState;
import io.hypersistence.tsid.TSID;

import java.math.BigDecimal;
import java.util.List;

public class Order {
    private String key;
    private String name;
    private Principal principal;
    private Long storeId;
    private BigDecimal totalPrice;
    private OrderState state;
    private List<OrderLine> lines;

    public Order(String key, String name, Principal principal, Long storeId, BigDecimal totalPrice, OrderState state, List<OrderLine> lines) {
        this.key = key;
        this.name = name;
        this.principal = principal;
        this.storeId = storeId;
        this.totalPrice = totalPrice;
        this.state = state;
        this.lines = lines;
    }

    public static Order create(String name, Principal principal, Long storeId, BigDecimal totalPrice) {
        return new Order(
                TSID.Factory.getTsid().toString(),
                name,
                principal,
                storeId,
                totalPrice,
                OrderState.CREATED,
                null
        );
    }

    public void fillOrderLines(List<OrderLine> lines) {
        this.lines = lines;
    }

    public void paid() {
        this.state = OrderState.PAID;
    }

    public void canceled() {
        this.state = OrderState.CANCELED;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public OrderState getState() {
        return state;
    }

    public List<OrderLine> getLines() {
        return lines;
    }

    public Long getStoreId() {
        return storeId;
    }
}
