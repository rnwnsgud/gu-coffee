package com.coffee.gu.coupon;

import com.coffee.gu.enums.EntityStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.coffee.gu.coupon.QCouponEntity.couponEntity;
import static com.coffee.gu.coupon.QIssuedCouponEntity.issuedCouponEntity;

@Repository
public class IssuedCouponRepositoryImpl implements IssuedCouponRepository{

    private final IssuedCouponJpaRepository issuedCouponJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    public IssuedCouponRepositoryImpl(IssuedCouponJpaRepository issuedCouponJpaRepository, JPAQueryFactory jpaQueryFactory) {
        this.issuedCouponJpaRepository = issuedCouponJpaRepository;
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public IssuedCoupon save(IssuedCoupon issuedCoupon) {
        return issuedCouponJpaRepository.save(IssuedCouponEntity.from(issuedCoupon)).toModel(issuedCoupon.getCoupon());
    }

    @Override
    public List<IssuedCoupon> saveAll(List<IssuedCoupon> issuedCoupons) {
        List<IssuedCouponEntity> entities = issuedCoupons.stream()
                .map(IssuedCouponEntity::from)
                .toList();
        List<IssuedCouponEntity> savedEntities = issuedCouponJpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(entity -> entity.toModel(issuedCoupons.get(savedEntities.indexOf(entity)).getCoupon()))
                .toList();
    }

    @Override
    public List<IssuedCoupon> findAllByPrincipalKey(String principalKey) {
        List<IssuedCouponEntity> issuedCoupons = jpaQueryFactory
                .selectFrom(issuedCouponEntity)
                .where(
                        issuedCouponEntity.principalKey.eq(principalKey),
                        issuedCouponEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetch();
        return getIssuedCoupons(issuedCoupons);
    }

    @Override
    public boolean existsByPrincipalKeyAndCouponId(String principalKey, Long couponId) {
        return jpaQueryFactory.selectFrom(issuedCouponEntity)
                .where(
                        issuedCouponEntity.principalKey.eq(principalKey),
                        issuedCouponEntity.couponId.eq(couponId),
                        issuedCouponEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetchFirst() != null;
    }

    @Override
    public Optional<IssuedCoupon> findById(Long issuedCouponId) {
        IssuedCouponEntity issuedCoupon = jpaQueryFactory.selectFrom(issuedCouponEntity)
                .where(
                        issuedCouponEntity.id.eq(issuedCouponId),
                        issuedCouponEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetchFirst();
        if (issuedCoupon == null) return Optional.empty();
        CouponEntity coupon = jpaQueryFactory.
                selectFrom(couponEntity)
                .where(couponEntity.id.eq(issuedCoupon.getCouponId()))
                .fetchFirst();
        return Optional.of(issuedCoupon.toModel(coupon.toModel()));
    }

    @Override
    public List<IssuedCoupon> findAllByIdIn(List<Long> issuedCouponIds) {
        List<IssuedCouponEntity> issuedCoupons = jpaQueryFactory
                .selectFrom(issuedCouponEntity)
                .where(
                        issuedCouponEntity.id.in(issuedCouponIds),
                        issuedCouponEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetch();
        return getIssuedCoupons(issuedCoupons);
    }

    private List<IssuedCoupon> getIssuedCoupons(List<IssuedCouponEntity> issuedCoupons) {
        if (issuedCoupons.isEmpty()) return List.of();
        List<Long> couponIds = issuedCoupons.stream()
                .map(IssuedCouponEntity::getCouponId)
                .distinct()
                .toList();
        List<CouponEntity> coupons = jpaQueryFactory
                .selectFrom(couponEntity)
                .where(couponEntity.id.in(couponIds))
                .fetch();
        Map<Long, Coupon> couponMap = coupons.stream()
                .collect(Collectors.toMap(CouponEntity::getId, CouponEntity::toModel));
        return issuedCoupons.stream()
                .map(entity -> {
                    Coupon coupon = couponMap.get(entity.getCouponId());
                    return entity.toModel(coupon);
                })
                .toList();
    }
}

