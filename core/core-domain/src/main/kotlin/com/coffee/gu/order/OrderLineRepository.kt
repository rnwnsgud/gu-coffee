package com.coffee.gu.order

interface OrderLineRepository {
    fun saveAll(orderLines: List<OrderLine>): List<OrderLine>
    fun findByOrderKey(orderKeys: Collection<String>): List<OrderLine>
    fun findByOrderKey(orderKey: String): List<OrderLine> {
        return findByOrderKey(listOf(orderKey))
    }
}