package com.coffee.gu.api.controller.v1;

import com.coffee.gu.OffsetLimit;
import com.coffee.gu.Page;
import com.coffee.gu.api.controller.v1.response.menu.MenuDetailResponse;
import com.coffee.gu.api.controller.v1.response.menu.MenuResponse;
import com.coffee.gu.menu.Menu;
import com.coffee.gu.menu.MenuDetailResult;
import com.coffee.gu.menu.MenuService;

import com.coffee.gu.response.ApiResponse;
import com.coffee.gu.response.PageResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/v1/menus")
    public ApiResponse<PageResponse<MenuResponse>> findMenus(
            @RequestParam Long categoryId,
            @RequestParam Integer offset,
            @RequestParam Integer limit) {
        Page<Menu> menus = menuService.findMenus(categoryId, new OffsetLimit(offset, limit));
        return ApiResponse.success(new PageResponse<>(MenuResponse.from(menus.content()), menus.hasNext(), null, null));
    }

    @GetMapping("/v2/menus")
    public ApiResponse<PageResponse<MenuResponse>> findMenus(
            @RequestParam(value = "categoryId") Long categoryId,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "cursor", required = false) LocalDateTime cursor,
            @RequestParam(value = "lastId", required = false) Long lastId) {
        Page<Menu> menus = menuService.findMenus(categoryId, pageSize, cursor, lastId);
        return ApiResponse.success(new PageResponse<>(MenuResponse.from(menus.content()), menus.hasNext(), menus.nextCursor(), menus.nextLastId()));
    }

    @GetMapping("/v1/menus/{menuId}")
    public ApiResponse<MenuDetailResponse> getMenu(@PathVariable Long menuId) {
        MenuDetailResult menu = menuService.getMenu(menuId);
        return ApiResponse.success(MenuDetailResponse.from(menu));
    }
}
