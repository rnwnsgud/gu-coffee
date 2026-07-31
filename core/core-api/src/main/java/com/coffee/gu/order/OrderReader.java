package com.coffee.gu.order;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.Principal;
import com.coffee.gu.enums.OrderState;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderReader {

    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;

    public OrderReader(OrderRepository orderRepository, OrderLineRepository orderLineRepository) {
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
    }

    public List<Order> findByPrincipal(Principal principal) {
        List<Order> orders = orderRepository.getByPrincipalKey(principal.getKey());
        if (orders.isEmpty()) return List.of();
        List<OrderLine> orderLines = orderLineRepository.findByOrderKey(orders.stream()
                .map(Order::getKey)
                .collect(Collectors.toSet()));
        Map<String, List<OrderLine>> orderLinesMap = orderLines.stream()
                .collect(Collectors.groupingBy(OrderLine::getOrderKey));
        orders.forEach(order -> {
            List<OrderLine> matchedLines = orderLinesMap.getOrDefault(order.getKey(), List.of());
            order.fillOrderLines(matchedLines);
        });
        return orders;
    }

    public Order getByOrderKey(String orderKey, OrderState state) {
        Order order = orderRepository.findByOrderKey(orderKey, state)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA, null));
        List<OrderLine> lines = orderLineRepository.findByOrderKey(orderKey);
        if (lines.isEmpty()) throw new CoreException(ErrorType.NOT_FOUND_DATA, null);
        order.fillOrderLines(lines);
        return order;
    }

    public Order getByOrderKey(String orderKey) {
        Order order = orderRepository.findByOrderKey(orderKey)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA, null));
        List<OrderLine> lines = orderLineRepository.findByOrderKey(order.getKey());
        order.fillOrderLines(lines);
        return order;
    }

}
