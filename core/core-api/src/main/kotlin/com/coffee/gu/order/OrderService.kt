package com.coffee.gu.order

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
        val menus = menuFinder.findAllByIdIn(newOrder.menuIds.toList())
        val orderMenus = OrderMenus.from(menus, newOrder.menuIds)
        val orderKey = Order.generateKey()
        val menuQuantities = newOrder.toMenuQuantityMap()
        limitedMenuStockManager.reserveIfLimited(
            orderKey = orderKey,
            principalKey = newOrder.principal.key,
            menuQuantities = menuQuantities,
        )
        return try {
            orderManager.create(newOrder, orderMenus, orderKey)
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
