package com.coffee.gu.stamp

import com.coffee.gu.stamp.QStampHistoryEntity.stampHistoryEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StampHistoryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
    private val stampHistoryJpaRepository: StampHistoryJpaRepository,
) : StampHistoryRepository {

    override fun save(stampHistory: StampHistory): StampHistory {
        return stampHistoryJpaRepository.save(StampHistoryEntity.from(stampHistory)).toModel()
    }

    override fun getWithinNMonths(principalKey: String, nMonthsAgo: LocalDateTime): List<StampHistory> {
        return queryFactory
            .selectFrom(stampHistoryEntity)
            .where(
                stampHistoryEntity.principalKey.eq(principalKey),
                stampHistoryEntity.createdAt.after(nMonthsAgo),
            )
            .orderBy(stampHistoryEntity.createdAt.desc())
            .fetch()
            .map { it.toModel() }
    }
}
