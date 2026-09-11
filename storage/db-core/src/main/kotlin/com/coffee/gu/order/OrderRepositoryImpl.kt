package com.coffee.gu.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.EntityStatus
import com.coffee.gu.enums.OrderState
import com.coffee.gu.order.QOrderEntity.orderEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
    private val jpaQueryFactory: JPAQueryFactory,
) : OrderRepository {

    override fun create(order: Order): Order {
        return orderJpaRepository.save(OrderEntity.create(order)).toModel()
    }

    override fun save(order: Order): Order {
        return orderJpaRepository.save(OrderEntity.from(order)).toModel()
    }

    override fun getByPrincipalKey(principalKey: String): List<Order> {
        return jpaQueryFactory
            .selectFrom(orderEntity)
            .where(
                orderEntity.principalKey.eq(principalKey),
                orderEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetch()
            .map { it.toModel() }
    }

    override fun findByOrderKey(orderKey: String): Order {
        return jpaQueryFactory
            .selectFrom(orderEntity)
            .where(
                orderEntity.orderKey.eq(orderKey),
                orderEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetchFirst()
            ?.toModel() ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
    }

    override fun findByOrderKey(orderKey: String, state: OrderState): Order {
        return jpaQueryFactory
            .selectFrom(orderEntity)
            .where(
                orderEntity.orderKey.eq(orderKey),
                orderEntity.state.eq(state),
                orderEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetchFirst()
            ?.toModel() ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
    }
}
