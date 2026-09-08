package com.coffee.gu.coupon

import com.coffee.gu.coupon.QCouponTargetEntity.couponTargetEntity
import com.coffee.gu.enums.CouponTargetType
import com.coffee.gu.enums.EntityStatus
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class CouponTargetRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory,
) : CouponTargetRepository {

    override fun findAllByTargetTypeAndTargetIdIn(
        targetType: CouponTargetType,
        targetIds: List<Long>,
    ): List<CouponTarget> {
        return jpaQueryFactory.selectFrom(couponTargetEntity)
            .where(
                couponTargetEntity.targetType.eq(targetType),
                couponTargetEntity.targetId.`in`(targetIds),
                couponTargetEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetch()
            .map { it.toModel() }
    }
}
