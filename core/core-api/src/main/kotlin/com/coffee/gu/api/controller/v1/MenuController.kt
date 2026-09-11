package com.coffee.gu.api.controller.v1

import com.coffee.gu.OffsetLimit
import com.coffee.gu.api.controller.v1.response.menu.MenuDetailResponse
import com.coffee.gu.api.controller.v1.response.menu.MenuResponse
import com.coffee.gu.menu.MenuService
import com.coffee.gu.response.ApiResponse
import com.coffee.gu.response.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class MenuController(
    private val menuService: MenuService,
) {
    @GetMapping("/v1/menus")
    fun findMenus(
        @RequestParam categoryId: Long,
        @RequestParam offset: Int,
        @RequestParam limit: Int,
    ): ApiResponse<PageResponse<MenuResponse>> {
        val menus = menuService.findMenus(categoryId, OffsetLimit(offset, limit))
        return ApiResponse.success(PageResponse(MenuResponse.from(menus.content), menus.hasNext, null, null))
    }

    @GetMapping("/v2/menus")
    fun findMenus(
        @RequestParam("categoryId") categoryId: Long,
        @RequestParam("pageSize", defaultValue = "10") pageSize: Int,
        @RequestParam("cursor") cursor: LocalDateTime?,
        @RequestParam("lastId") lastId: Long?,
    ): ApiResponse<PageResponse<MenuResponse>> {
        val menus = menuService.findMenus(categoryId, pageSize, cursor ?: LocalDateTime.MIN, lastId ?: 0L)
        return ApiResponse.success(PageResponse(MenuResponse.from(menus.content), menus.hasNext, menus.nextCursor, menus.nextLastId))
    }

    @GetMapping("/v1/menus/{menuId}")
    fun getMenu(@PathVariable menuId: Long): ApiResponse<MenuDetailResponse> {
        val menu = menuService.getMenu(menuId)
        return ApiResponse.success(MenuDetailResponse.from(menu))
    }
}
