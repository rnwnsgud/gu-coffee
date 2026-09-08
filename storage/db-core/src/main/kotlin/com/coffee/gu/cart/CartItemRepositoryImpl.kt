package com.coffee.gu.cart

import com.coffee.gu.Principal
import com.coffee.gu.cart.QCartItemEntity.cartItemEntity
import com.coffee.gu.enums.EntityStatus
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class CartItemRepositoryImpl(
    private val cartItemJpaRepository: CartItemJpaRepository,
    private val jpaQueryFactory: JPAQueryFactory,
) : CartItemRepository {

    override fun findByPrincipalKey(principalKey: String): List<CartItem> {
        return jpaQueryFactory.selectFrom(cartItemEntity)
            .where(
                cartItemEntity.principalKey.eq(principalKey),
                cartItemEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetch()
            .map { it.toModel() }
    }

    override fun findByPrincipalIncludingDeleted(principalKey: String): Optional<CartItem> {
        return Optional.ofNullable(
            jpaQueryFactory.selectFrom(cartItemEntity)
                .where(cartItemEntity.principalKey.eq(principalKey))
                .fetchFirst(),
        ).map { it.toModel() }
    }

    override fun save(cartItem: CartItem, principal: Principal): CartItem {
        val entity = CartItemEntity.of(cartItem, principal)
        return cartItemJpaRepository.save(entity).toModel()
    }
}
