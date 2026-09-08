package com.coffee.gu.store

import com.coffee.gu.enums.EntityStatus
import com.coffee.gu.store.QSalesHourEntity.salesHourEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class SalesHourRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : SalesHourRepository {

    override fun findAllByStores(stores: Collection<Store>): List<SalesHour> {
        val storeIds = stores.map { it.id }
        return queryFactory.selectFrom(salesHourEntity)
            .where(
                salesHourEntity.storeId.`in`(storeIds),
                salesHourEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetch()
            .map { it.toModel() }
    }
}
