package com.coffee.gu.order;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.Principal;
import com.coffee.gu.menu.Menu;
import com.coffee.gu.menu.MenuFinder;
import com.coffee.gu.enums.OrderState;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class OrderService {

    private final OrderManager orderManager;
    private final OrderReader orderReader;
    private final MenuFinder menuFinder;

    public OrderService(OrderManager orderManager, OrderReader orderReader, MenuFinder menuFinder) {
        this.orderManager = orderManager;
        this.orderReader = orderReader;
        this.menuFinder = menuFinder;
    }

    public String create(NewOrder newOrder) {
        Set<Long> orderMenuIds = newOrder.lines().stream()
                .map(NewOrderLine::menuId)
                .collect(Collectors.toSet());
        List<Menu> menus = menuFinder.findAllByIdIn(orderMenuIds.stream().toList());
        OrderMenus orderMenus = OrderMenus.from(menus);
        if (menus.isEmpty()) throw new CoreException(ErrorType.NOT_FOUND_DATA, null);
        if (!orderMenus.matches(orderMenuIds)) throw new CoreException(ErrorType.MENU_MISMATCH_IN_ORDER, null);
        return orderManager.create(newOrder.principal(), newOrder, orderMenus);
    }

    public List<OrderSummary> getPaidOrders(Principal principal) {
        return orderReader.findByPrincipal(principal)
                .stream()
                .filter(order -> order.getState() == OrderState.PAID)
                .map(order -> new OrderSummary(
                        order.getKey(),
                        order.getName(),
                        principal,
                        order.getTotalPrice(),
                        order.getState()
                )).toList();
    }

    public Order getOrder(String orderKey, OrderState state) {
        return orderReader.getByOrderKey(orderKey, state);
    }

    public Order getOrder(String orderKey) {
        return orderReader.getByOrderKey(orderKey);
    }

}
