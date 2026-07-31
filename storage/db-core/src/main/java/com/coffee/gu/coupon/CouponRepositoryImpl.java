package com.coffee.gu.coupon;

import com.coffee.gu.enums.EntityStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.coffee.gu.coupon.QCouponEntity.couponEntity;

@Repository
public class CouponRepositoryImpl implements CouponRepository{

    private final JPAQueryFactory jpaQueryFactory;

    public CouponRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<Coupon> findAllByIdIn(List<Long> couponIds) {
        return jpaQueryFactory.selectFrom(couponEntity)
                .where(
                        couponEntity.id.in(couponIds),
                        couponEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetch()
                .stream()
                .map(CouponEntity::toModel)
                .toList();
    }

    @Override
    public Optional<Coupon> findById(Long couponId) {
        return Optional.ofNullable(
                jpaQueryFactory.selectFrom(couponEntity)
                        .where(
                                couponEntity.id.eq(couponId),
                                couponEntity.entityStatus.eq(EntityStatus.ACTIVE)
                        )
                        .fetchFirst()
        ).map(CouponEntity::toModel);
    }
}
