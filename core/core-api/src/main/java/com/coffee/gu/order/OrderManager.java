package com.coffee.gu.order;

import com.coffee.gu.Principal;
import com.coffee.gu.menu.Menu;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


@Component
public class OrderManager {

    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;

    public OrderManager(OrderRepository orderRepository, OrderLineRepository orderLineRepository) {
        this.orderRepository = orderRepository;
        this.orderLineRepository = orderLineRepository;
    }

    @Transactional
    public String create(Principal principal, NewOrder newOrder, OrderMenus orderMenus) {
        NewOrderLine firstLine = newOrder.lines().getFirst();
        Menu menu = orderMenus.getByMenuId(firstLine.menuId());
        BigDecimal totalPrice = newOrder.lines().stream()
                .map(line -> orderMenus.getByMenuId(line.menuId())
                        .getSalesPrice()
                        .multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Order order = orderRepository.create(Order.create(
                createOrderName(menu, newOrder),
                principal,
                newOrder.storeId(),
                totalPrice
        ));
        orderLineRepository.saveAll(
                newOrder.lines().stream()
                        .map(line -> OrderLine.create(
                                        order.getKey(),
                                        line.menuId(),
                                        orderMenus.getByMenuId(line.menuId()).getName(),
                                        orderMenus.getByMenuId(line.menuId()).getImageUrl(),
                                        orderMenus.getByMenuId(line.menuId()).getDescription(),
                                        line.quantity(),
                                        orderMenus.getByMenuId(line.menuId()).getSalesPrice(),
                                        orderMenus.getByMenuId(line.menuId()).getSalesPrice().multiply(BigDecimal.valueOf(line.quantity())),
                                        orderMenus.getByMenuId(line.menuId()).isStampEligible()
                                )
                        ).toList());
        return order.getKey();
    }

    public void pay(Order order) {
        order.paid();
        orderRepository.save(order);
    }

    public void cancel(Order order) {
        order.canceled();
        orderRepository.save(order);
    }

    private String createOrderName(Menu firstMenu, NewOrder newOrder) {
        if (newOrder.lines().size() == 1) {
            return firstMenu.getName();
        }
        return firstMenu.getName() + " 외 " + (newOrder.lines().size() - 1) + "개";
    }
}
