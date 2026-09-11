package com.coffee.gu.order

import org.springframework.data.jpa.repository.JpaRepository

interface OrderLineJpaRepository : JpaRepository<OrderLineEntity, Long> {
    fun findAllByOrderKeyIn(orderKeys: Collection<String>): List<OrderLineEntity>
    fun findByOrderKey(orderKey: String): List<OrderLineEntity>
}
