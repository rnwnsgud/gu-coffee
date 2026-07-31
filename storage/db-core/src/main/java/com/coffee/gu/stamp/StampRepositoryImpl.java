package com.coffee.gu.stamp;

import com.coffee.gu.enums.StampState;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.coffee.gu.stamp.QStampEntity.stampEntity;

@Repository
public class StampRepositoryImpl implements StampRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final StampJpaRepository stampJpaRepository;

    public StampRepositoryImpl(JPAQueryFactory jpaQueryFactory, StampJpaRepository stampJpaRepository) {
        this.jpaQueryFactory = jpaQueryFactory;
        this.stampJpaRepository = stampJpaRepository;
    }

    @Override
    public Stamp save(Stamp stamp) {
        return stampJpaRepository.save(StampEntity.from(stamp)).toModel();
    }

    @Override
    public List<Stamp> saveAll(Collection<Stamp> stamps) {
        return stampJpaRepository.saveAll(stamps.stream().map(StampEntity::from).toList()).stream().map(StampEntity::toModel).toList();
    }

    @Override
    public Long countAvailableStamps(String principalKey, LocalDateTime now) {
        return Objects.requireNonNullElse(
                jpaQueryFactory
                        .select(stampEntity.count())
                        .from(stampEntity)
                        .where(stampEntity.principalKey.eq(principalKey),
                                stampEntity.state.eq(StampState.EARNED),
                                stampEntity.expiredAt.after(now)
                        )
                        .fetchOne(),
                0L
        );
    }

    @Override
    public List<Stamp> getAvailableStamps(String principalKey, LocalDateTime now, int pageNumber, int pageSize) {
        return jpaQueryFactory
                .selectFrom(stampEntity)
                .where(stampEntity.principalKey.eq(principalKey),
                        stampEntity.state.eq(StampState.EARNED),
                        stampEntity.expiredAt.after(now)
                )
                .offset((long) pageNumber * pageSize)
                .limit(pageSize)
                .orderBy(stampEntity.createdAt.asc())
                .fetch()
                .stream()
                .map(StampEntity::toModel)
                .toList();
    }

    @Override
    public Long countExpiringStamps(String principalKey, LocalDateTime now, LocalDateTime nDaysLater) {
        return Objects.requireNonNullElse(
                jpaQueryFactory
                        .select(stampEntity.count())
                        .from(stampEntity)
                        .where(stampEntity.principalKey.eq(principalKey),
                                stampEntity.state.eq(StampState.EARNED),
                                stampEntity.expiredAt.between(now, nDaysLater)
                        )
                        .fetchOne(),
                0L
        );

    }

    @Override
    public List<Stamp> findByOrderKey(String orderKey) {
        return jpaQueryFactory
                .selectFrom(stampEntity)
                .where(stampEntity.orderKey.eq(orderKey))
                .fetch()
                .stream()
                .map(StampEntity::toModel)
                .toList();
    }
}
