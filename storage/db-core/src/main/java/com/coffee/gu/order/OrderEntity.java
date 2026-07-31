package com.coffee.gu.order;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.OrderState;
import com.coffee.gu.enums.PrincipalType;
import com.coffee.gu.BaseCustomIdEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "`order`")
public class OrderEntity extends BaseCustomIdEntity<String> {

    private String name;
    private String principalKey;
    @Enumerated(EnumType.STRING)
    private PrincipalType principalType;
    private Long storeId;
    private BigDecimal totalPrice;
    private OrderState state;

    public OrderEntity() {}

    private OrderEntity(String key, String name, String principalKey, PrincipalType principalType, Long storeId, BigDecimal totalPrice, OrderState state, boolean isNewEntity) {
        super(key, isNewEntity);
        this.name = name;
        this.principalKey = principalKey;
        this.principalType = principalType;
        this.storeId = storeId;
        this.state = state;
        this.totalPrice = totalPrice;
    }

    public static OrderEntity create(Order order) {
        return new OrderEntity(
                order.getKey(),
                order.getName(),
                order.getPrincipal().getKey(),
                order.getPrincipal().getType(),
                order.getStoreId(),
                order.getTotalPrice(),
                order.getState(),
                true);
    }

    public static OrderEntity from(Order order) {
        return new OrderEntity(
                order.getKey(),
                order.getName(),
                order.getPrincipal().getKey(),
                order.getPrincipal().getType(),
                order.getStoreId(),
                order.getTotalPrice(),
                order.getState(),
                false);
    }

    public Order toModel() {
        return new Order(
                this.id,
                this.name,
                new Principal(this.principalKey, this.principalType),
                this.storeId,
                this.totalPrice,
                this.state,
                null
        );
    }


    public String getName() {
        return name;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public OrderState getState() {
        return state;
    }

    public String getPrincipalKey() {
        return principalKey;
    }

    public PrincipalType getPrincipalType() {
        return principalType;
    }

    public Long getStoreId() {
        return storeId;
    }
}
