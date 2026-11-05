package com.coffee.gu.api.controller.v1

import com.coffee.gu.api.controller.v1.response.menu.LimitedMenuResponse
import com.coffee.gu.menu.LimitedMenuService
import com.coffee.gu.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class LimitedMenuController(
    private val limitedMenuService: LimitedMenuService,
) {
    @GetMapping("/v1/limited-menus/{menuId}")
    fun getLimitedMenu(@PathVariable menuId: Long): ApiResponse<LimitedMenuResponse> {
        val limitedMenu = limitedMenuService.getByMenuId(menuId)
        val availableStock = limitedMenuService.getAvailableStock(menuId)
        return ApiResponse.success(LimitedMenuResponse.of(limitedMenu, availableStock))
    }
}
