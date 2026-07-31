package com.coffee.gu.order;

import com.coffee.gu.enums.EntityStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

import static com.coffee.gu.order.QOrderLineEntity.*;

@Repository
public class OrderLineRepositoryImpl implements OrderLineRepository {

    private final OrderLineJpaRepository orderLineJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    public OrderLineRepositoryImpl(OrderLineJpaRepository orderLineJpaRepository, JPAQueryFactory jpaQueryFactory) {
        this.orderLineJpaRepository = orderLineJpaRepository;
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<OrderLine> saveAll(List<OrderLine> orderLines) {
        return orderLineJpaRepository.saveAll(
                orderLines.stream().map(OrderLineEntity::from).toList()
        ).stream().map(OrderLineEntity::toModel).toList();
    }

    @Override
    public List<OrderLine> findByOrderKey(Collection<String> orderKeys) {
        return jpaQueryFactory.selectFrom(orderLineEntity)
                .where(
                        orderLineEntity.orderKey.in(orderKeys),
                        orderLineEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetch()
                .stream()
                .map(OrderLineEntity::toModel)
                .toList();
    }
}
