package com.coffee.gu.order;

import com.coffee.gu.enums.OrderState;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order create(Order order);
    Order save(Order order);
    List<Order> getByPrincipalKey(String principalKey);
    Optional<Order> findByOrderKey(String orderKey);
    Optional<Order> findByOrderKey(String orderKey, OrderState state);
}
