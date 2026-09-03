package com.coffee.gu.order

import com.coffee.gu.enums.OrderState

interface OrderRepository {
    fun create(order: Order): Order
    fun save(order: Order): Order
    fun getByPrincipalKey(principalKey: String): List<Order>
    fun findByOrderKey(orderKey: String): Order?
    fun findByOrderKey(orderKey: String, state: OrderState): Order?
}
