package com.coffee.gu.coupon;

import com.coffee.gu.enums.CouponTargetType;
import com.coffee.gu.enums.EntityStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.coffee.gu.coupon.QCouponTargetEntity.couponTargetEntity;

@Repository
public class CouponTargetRepositoryImpl implements CouponTargetRepository{

    private final JPAQueryFactory jpaQueryFactory;

    public CouponTargetRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<CouponTarget> findAllByTargetTypeAndTargetIdIn(CouponTargetType targetType, List<Long> targetIds) {
        return jpaQueryFactory.selectFrom(couponTargetEntity)
                .where(
                        couponTargetEntity.targetType.eq(targetType),
                        couponTargetEntity.targetId.in(targetIds),
                        couponTargetEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetch()
                .stream()
                .map(CouponTargetEntity::toModel)
                .toList();
    }
}
