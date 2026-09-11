package com.coffee.gu.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderState
import org.springframework.stereotype.Component

@Component
class OrderReader(
    private val orderRepository: OrderRepository,
    private val orderLineRepository: OrderLineRepository,
) {
    fun findByPrincipal(principal: Principal): List<Order> {
        val orders = orderRepository.getByPrincipalKey(principal.key)
        if (orders.isEmpty()) return emptyList()
        val orderKeys = orders.map { it.key }.toSet()
        val orderLines = orderLineRepository.findByOrderKey(orderKeys)
        val orderLinesMap = orderLines.groupBy { it.orderKey }
        orders.forEach { order ->
            val matchedLines = orderLinesMap[order.key] ?: emptyList()
            order.fillOrderLines(matchedLines)
        }
        return orders
    }

    fun getByOrderKey(orderKey: String, state: OrderState): Order {
        val order = orderRepository.findByOrderKey(orderKey, state)
        val lines = orderLineRepository.findByOrderKey(orderKey)
        if (lines.isEmpty()) throw CoreException(ErrorType.NOT_FOUND_DATA)
        order.fillOrderLines(lines)
        return order
    }

    fun getByOrderKey(orderKey: String): Order {
        val order = orderRepository.findByOrderKey(orderKey)
        val lines = orderLineRepository.findByOrderKey(order.key)
        order.fillOrderLines(lines)
        return order
    }
}
