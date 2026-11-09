package com.coffee.gu.order

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderManager(
    private val orderRepository: OrderRepository,
    private val orderLineRepository: OrderLineRepository,
) {
    @Transactional
    fun create(
        newOrder: NewOrder,
        orderMenus: OrderMenus,
        orderKey: String = Order.generateKey(),
    ): String {
        val order = Order.create(newOrder, orderMenus, orderKey)
        val savedOrder = orderRepository.create(order)
        orderLineRepository.saveAll(order.lines)
        return savedOrder.key
    }

    fun pay(order: Order) {
        order.paid()
        orderRepository.save(order)
    }

    fun cancel(order: Order) {
        order.canceled()
        orderRepository.save(order)
    }
}
