package com.coffee.gu.store;

import com.coffee.gu.enums.EntityStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

import static com.coffee.gu.store.QSalesHourEntity.salesHourEntity;

@Repository
public class SalesHourRepositoryImpl implements SalesHourRepository {

    private final JPAQueryFactory queryFactory;

    public SalesHourRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<SalesHour> findAllByStores(Collection<Store> stores) {
        List<Long> storeIds = stores.stream().map(Store::getId).toList();
        return queryFactory.selectFrom(salesHourEntity)
                .where(
                        salesHourEntity.storeId.in(storeIds),
                        salesHourEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetch()
                .stream()
                .map(SalesHourEntity::toModel)
                .toList();
    }
}
