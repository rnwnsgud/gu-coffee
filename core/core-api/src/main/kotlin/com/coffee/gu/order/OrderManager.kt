package com.coffee.gu.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.menu.Menu
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Component
class OrderManager(
    private val orderRepository: OrderRepository,
    private val orderLineRepository: OrderLineRepository,
) {
    @Transactional
    fun create(principal: Principal, newOrder: NewOrder, orderMenus: OrderMenus): String {
        val firstLine = newOrder.lines.first()
        val menu = orderMenus.getByMenuId(firstLine.menuId) ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        val totalPrice = newOrder.lines.fold(BigDecimal.ZERO) { acc, line ->
            val salesPrice = orderMenus.getByMenuId(line.menuId)?.salesPrice ?: BigDecimal.ZERO
            acc.add(salesPrice.multiply(BigDecimal.valueOf(line.quantity)))
        }
        val order = orderRepository.create(
            Order.create(
                createOrderName(menu, newOrder),
                principal,
                newOrder.storeId,
                totalPrice
            )
        )
        orderLineRepository.saveAll(
            newOrder.lines.map { line ->
                val lineMenu = orderMenus.getByMenuId(line.menuId) ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
                val salesPrice = lineMenu.salesPrice ?: BigDecimal.ZERO
                OrderLine.create(
                    order.key,
                    line.menuId,
                    lineMenu.name ?: "",
                    lineMenu.imageUrl ?: "",
                    lineMenu.description ?: "",
                    line.quantity,
                    salesPrice,
                    salesPrice.multiply(BigDecimal.valueOf(line.quantity)),
                    lineMenu.isStampEligible
                )
            }
        )
        return order.key
    }

    fun pay(order: Order) {
        order.paid()
        orderRepository.save(order)
    }

    fun cancel(order: Order) {
        order.canceled()
        orderRepository.save(order)
    }

    private fun createOrderName(firstMenu: Menu, newOrder: NewOrder): String {
        val name = firstMenu.name ?: ""
        if (newOrder.lines.size == 1) {
            return name
        }
        return "$name 외 ${newOrder.lines.size - 1}개"
    }
}
