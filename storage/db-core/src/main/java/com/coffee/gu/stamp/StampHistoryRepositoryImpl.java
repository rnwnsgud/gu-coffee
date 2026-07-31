package com.coffee.gu.stamp;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.coffee.gu.stamp.QStampHistoryEntity.stampHistoryEntity;

@Component
public class StampHistoryRepositoryImpl implements StampHistoryRepository {

    private final JPAQueryFactory queryFactory;
    private final StampHistoryJpaRepository stampHistoryJpaRepository;

    public StampHistoryRepositoryImpl(JPAQueryFactory queryFactory, StampHistoryJpaRepository stampHistoryJpaRepository) {
        this.queryFactory = queryFactory;
        this.stampHistoryJpaRepository = stampHistoryJpaRepository;
    }

    @Override
    public StampHistory save(StampHistory stampHistory) {
        return stampHistoryJpaRepository.save(StampHistoryEntity.from(stampHistory)).toModel();
    }

    @Override
    public List<StampHistory> getWithinNMonths(String principalKey, LocalDateTime nMonthsAgo) {
        return queryFactory
                .selectFrom(stampHistoryEntity)
                .where(stampHistoryEntity.principalKey.eq(principalKey)
                        .and(stampHistoryEntity.createdAt.after(nMonthsAgo)))
                .orderBy(stampHistoryEntity.createdAt.desc())
                .fetch()
                .stream()
                .map(StampHistoryEntity::toModel)
                .toList();
    }
}
