package com.coffee.gu.coupon

import com.coffee.gu.coupon.QCouponEntity.couponEntity
import com.coffee.gu.enums.EntityStatus
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class CouponRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : CouponRepository {

    override fun findAllByIdIn(couponIds: List<Long>): List<Coupon> {
        return queryFactory.selectFrom(couponEntity)
            .where(
                couponEntity.id.`in`(couponIds),
                couponEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetch()
            .map { it.toModel() }
    }

    override fun findById(couponId: Long): Optional<Coupon> {
        return Optional.ofNullable(
            queryFactory.selectFrom(couponEntity)
                .where(
                    couponEntity.id.eq(couponId),
                    couponEntity.entityStatus.eq(EntityStatus.ACTIVE),
                )
                .fetchFirst(),
        ).map { it.toModel() }
    }
}
