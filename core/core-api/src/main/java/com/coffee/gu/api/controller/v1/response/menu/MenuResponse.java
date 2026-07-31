package com.coffee.gu.api.controller.v1.response.menu;

import com.coffee.gu.menu.Menu;

import java.math.BigDecimal;
import java.util.List;

public record MenuResponse(
    Long id,
    String name,
    BigDecimal salesPrice
) {
    public static List<MenuResponse> from(List<Menu> menus) {
        return menus.stream()
                .map(menu -> new MenuResponse(menu.getId(), menu.getName(), menu.getSalesPrice()))
                .toList();
    }
}
