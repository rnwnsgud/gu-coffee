package com.coffee.gu.stamp

import com.coffee.gu.stamp.QStampCouponUsageEntity.stampCouponUsageEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class StampCouponUsageRepositoryImpl(
    private val stampCouponUsageJpaRepository: StampCouponUsageJpaRepository,
    private val jpaQueryFactory: JPAQueryFactory,
) : StampCouponUsageRepository {

    override fun save(stampCouponUsage: StampCouponUsage): StampCouponUsage {
        return stampCouponUsageJpaRepository.save(StampCouponUsageEntity.from(stampCouponUsage)).toModel()
    }

    override fun saveAll(stampCouponUsages: List<StampCouponUsage>): List<StampCouponUsage> {
        return stampCouponUsageJpaRepository.saveAll(
            stampCouponUsages.map { StampCouponUsageEntity.from(it) }
        ).map { it.toModel() }
    }

    override fun findAllByStampIdIn(stampIds: List<Long>): List<StampCouponUsage> {
        return jpaQueryFactory.selectFrom(stampCouponUsageEntity)
            .where(stampCouponUsageEntity.stampId.`in`(stampIds))
            .fetch()
            .map { it.toModel() }
    }
}
