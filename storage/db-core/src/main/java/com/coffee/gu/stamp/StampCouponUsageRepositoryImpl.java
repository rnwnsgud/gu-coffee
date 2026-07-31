package com.coffee.gu.stamp;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.coffee.gu.stamp.QStampCouponUsageEntity.*;

@Repository
public class StampCouponUsageRepositoryImpl implements StampCouponUsageRepository {

    private final StampCouponUsageJpaRepository stampCouponUsageJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    public StampCouponUsageRepositoryImpl(StampCouponUsageJpaRepository stampCouponUsageJpaRepository, JPAQueryFactory jpaQueryFactory) {
        this.stampCouponUsageJpaRepository = stampCouponUsageJpaRepository;
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public StampCouponUsage save(StampCouponUsage stampCouponUsage) {
        return stampCouponUsageJpaRepository.save(StampCouponUsageEntity.from(stampCouponUsage)).toModel();
    }

    @Override
    public List<StampCouponUsage> saveAll(List<StampCouponUsage> stampCouponUsages) {
        return stampCouponUsageJpaRepository.saveAll(
                        stampCouponUsages.stream()
                                .map(StampCouponUsageEntity::from)
                                .toList()
                )
                .stream()
                .map(StampCouponUsageEntity::toModel)
                .toList();
    }

    @Override
    public List<StampCouponUsage> findAllByStampIdIn(List<Long> stampIds) {
        return jpaQueryFactory.selectFrom(stampCouponUsageEntity)
                .where(stampCouponUsageEntity.stampId.in(stampIds))
                .fetch()
                .stream()
                .map(StampCouponUsageEntity::toModel)
                .toList();
    }
}
