package com.coffee.gu.cart;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.EntityStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.coffee.gu.cart.QCartItemEntity.*;

@Repository
public class CartItemRepositoryImpl implements CartItemRepository{

    private final JPAQueryFactory jpaQueryFactory;
    private final CartItemJpaRepository cartItemJpaRepository;

    public CartItemRepositoryImpl(JPAQueryFactory jpaQueryFactory, CartItemJpaRepository cartItemJpaRepository) {
        this.jpaQueryFactory = jpaQueryFactory;
        this.cartItemJpaRepository = cartItemJpaRepository;
    }

    @Override
    public List<CartItem> findByPrincipalKey(String principalKey) {
        return jpaQueryFactory
                .selectFrom(cartItemEntity)
                .where(
                        cartItemEntity.principalKey.eq(principalKey),
                        cartItemEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetch()
                .stream()
                .map(CartItemEntity::toModel)
                .toList();
    }

    @Override
    public Optional<CartItem> findByPrincipalIncludingDeleted(String principalKey) {
        return jpaQueryFactory
                .selectFrom(cartItemEntity)
                .where(
                        cartItemEntity.principalKey.eq(principalKey)
                )
                .fetch()
                .stream()
                .map(CartItemEntity::toModel)
                .toList()
                .stream()
                .findFirst();
    }

    @Override
    public CartItem save(CartItem cartItem, Principal principal) {
        return cartItemJpaRepository.save(CartItemEntity.of(cartItem, principal)).toModel();
    }
}
