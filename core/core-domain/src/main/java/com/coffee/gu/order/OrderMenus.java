package com.coffee.gu.order;

import com.coffee.gu.menu.Menu;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OrderMenus {
    private final Map<Long, Menu> menuMap;

    private OrderMenus(Map<Long, Menu> menuMap) {
        this.menuMap = menuMap;
    }

    public static OrderMenus from(List<Menu> menus) {
        return new OrderMenus(menus.stream()
                .collect(Collectors.toMap(Menu::getId, menu -> menu)));
    }

    public Menu getByMenuId(Long menuId) {
        return menuMap.get(menuId);
    }

    public boolean matches(Set<Long> menuIds) {
        return menuMap.keySet().equals(menuIds);
    }
}
