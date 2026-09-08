package com.coffee.gu.order

import com.coffee.gu.enums.EntityStatus
import com.coffee.gu.order.QOrderLineEntity.orderLineEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class OrderLineRepositoryImpl(
    private val orderLineJpaRepository: OrderLineJpaRepository,
    private val jpaQueryFactory: JPAQueryFactory,
) : OrderLineRepository {

    override fun saveAll(orderLines: List<OrderLine>): List<OrderLine> {
        val entities = orderLines.map { OrderLineEntity.from(it) }
        return orderLineJpaRepository.saveAll(entities).map { it.toModel() }
    }

    override fun findByOrderKey(orderKeys: Collection<String>): List<OrderLine> {
        return jpaQueryFactory
            .selectFrom(orderLineEntity)
            .where(
                orderLineEntity.orderKey.`in`(orderKeys),
                orderLineEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetch()
            .map { it.toModel() }
    }
}
