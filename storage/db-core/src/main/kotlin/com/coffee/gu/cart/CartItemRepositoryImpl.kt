package com.coffee.gu.cart

import com.coffee.gu.Principal
import com.coffee.gu.cart.QCartItemEntity.cartItemEntity
import com.coffee.gu.enums.EntityStatus
import com.coffee.gu.menu.MenuRepository
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class CartItemRepositoryImpl(
    private val cartItemJpaRepository: CartItemJpaRepository,
    private val jpaQueryFactory: JPAQueryFactory,
    private val menuRepository: MenuRepository,
) : CartItemRepository {

    override fun findByPrincipalKey(principalKey: String): List<CartItem> {
        return jpaQueryFactory.selectFrom(cartItemEntity)
            .where(
                cartItemEntity.principalKey.eq(principalKey),
                cartItemEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetch()
            .map { it.toModel(menuRepository.findById(it.menuId)) }
    }

    override fun findByPrincipalKeyAndMenuIdIncludingDeleted(principalKey: String, menuId: Long): CartItem? {
        val cartItem = jpaQueryFactory.selectFrom(cartItemEntity)
            .where(cartItemEntity.principalKey.eq(principalKey), cartItemEntity.menuId.eq(menuId))
            .fetchFirst()

        return cartItem?.toModel(menuRepository.findById(cartItem.menuId))
    }

    override fun save(cartItem: CartItem, principal: Principal): CartItem {
        val entity = CartItemEntity.of(cartItem, principal)
        return cartItemJpaRepository.save(entity).toModel(menuRepository.findById(entity.menuId))
    }
}
