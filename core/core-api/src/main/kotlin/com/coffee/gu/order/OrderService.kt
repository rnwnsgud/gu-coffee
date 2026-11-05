package com.coffee.gu.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderState
import com.coffee.gu.menu.LimitedMenuStockManager
import com.coffee.gu.menu.MenuFinder
import org.springframework.stereotype.Service

@Service
class OrderService(
    private val orderManager: OrderManager,
    private val orderReader: OrderReader,
    private val menuFinder: MenuFinder,
    private val limitedMenuStockManager: LimitedMenuStockManager,
) {
    fun create(newOrder: NewOrder): String {
        val orderMenuIds = newOrder.lines.map { it.menuId }.toSet()
        val menus = menuFinder.findAllByIdIn(orderMenuIds.toList())
        val orderMenus = OrderMenus.from(menus)
        if (menus.isEmpty()) throw CoreException(ErrorType.NOT_FOUND_DATA)
        if (!orderMenus.matches(orderMenuIds)) throw CoreException(ErrorType.MENU_MISMATCH_IN_ORDER)
        val orderKey = Order.generateKey()
        val menuQuantities = newOrder.toMenuQuantityMap()
        limitedMenuStockManager.reserveIfLimited(
            orderKey = orderKey,
            principalKey = newOrder.principal.key,
            menuQuantities = menuQuantities,
        )
        return try {
            orderManager.create(newOrder.principal, newOrder, orderMenus, orderKey)
        } catch (e: Exception) {
            limitedMenuStockManager.releaseIfLimited(
                orderKey = orderKey,
                principalKey = newOrder.principal.key,
                menuQuantities = menuQuantities,
                restoreDbSoldQuantity = false,
            )
            throw e
        }
    }

    fun getPaidOrders(principal: Principal): List<OrderSummary> {
        return orderReader.findByPrincipal(principal)
            .filter { order -> order.state == OrderState.PAID }
            .map { OrderSummary(it.key, it.name, principal, it.totalPrice, it.state) }
    }

    fun getOrder(orderKey: String, state: OrderState): Order {
        return orderReader.getByOrderKey(orderKey, state)
    }

    fun getOrder(orderKey: String): Order {
        return orderReader.getByOrderKey(orderKey)
    }
}
