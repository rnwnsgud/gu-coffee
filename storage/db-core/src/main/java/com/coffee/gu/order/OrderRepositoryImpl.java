package com.coffee.gu.order;

import com.coffee.gu.enums.EntityStatus;
import com.coffee.gu.enums.OrderState;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.coffee.gu.order.QOrderEntity.*;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    public OrderRepositoryImpl(OrderJpaRepository orderJpaRepository, JPAQueryFactory jpaQueryFactory) {
        this.orderJpaRepository = orderJpaRepository;
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public Order create(Order order) {
        return orderJpaRepository.save(OrderEntity.create(order)).toModel();
    }

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(OrderEntity.from(order)).toModel();
    }

    @Override
    public List<Order> getByPrincipalKey(String principalKey) {
        return jpaQueryFactory.selectFrom(orderEntity)
                .where(
                        orderEntity.principalKey.eq(principalKey),
                        orderEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetch()
                .stream()
                .map(OrderEntity::toModel)
                .toList();
    }

    @Override
    public Optional<Order> findByOrderKey(String orderKey) {
        return Optional.ofNullable(
                jpaQueryFactory.selectFrom(orderEntity)
                        .where(
                                orderEntity.id.eq(orderKey),
                                orderEntity.entityStatus.eq(EntityStatus.ACTIVE)
                        )
                        .fetchFirst()
        ).map(OrderEntity::toModel);
    }

    @Override
    public Optional<Order> findByOrderKey(String orderKey, OrderState state) {
        return Optional.ofNullable(
                jpaQueryFactory.selectFrom(orderEntity)
                        .where(
                                orderEntity.id.eq(orderKey),
                                orderEntity.state.eq(state),
                                orderEntity.entityStatus.eq(EntityStatus.ACTIVE)
                        )
                        .fetchFirst()
        ).map(OrderEntity::toModel);
    }

}
