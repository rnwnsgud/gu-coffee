package com.coffee.gu.stamp

import com.coffee.gu.enums.StampState
import com.coffee.gu.stamp.QStampEntity.stampEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StampRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory,
    private val stampJpaRepository: StampJpaRepository,
) : StampRepository {

    override fun save(stamp: Stamp): Stamp {
        return stampJpaRepository.save(StampEntity.from(stamp)).toModel()
    }

    override fun saveAll(stamps: Collection<Stamp>): List<Stamp> {
        return stampJpaRepository.saveAll(stamps.map { StampEntity.from(it) })
            .map { it.toModel() }
    }

    override fun countAvailableStamps(principalKey: String, now: LocalDateTime): Long {
        return jpaQueryFactory
            .select(stampEntity.count())
            .from(stampEntity)
            .where(
                stampEntity.principalKey.eq(principalKey),
                stampEntity.state.eq(StampState.EARNED),
                stampEntity.expiredAt.after(now),
            )
            .fetchOne() ?: 0L
    }

    override fun getAvailableStamps(
        principalKey: String,
        now: LocalDateTime,
        pageNumber: Int,
        pageSize: Int,
    ): List<Stamp> {
        return jpaQueryFactory
            .selectFrom(stampEntity)
            .where(
                stampEntity.principalKey.eq(principalKey),
                stampEntity.state.eq(StampState.EARNED),
                stampEntity.expiredAt.after(now),
            )
            .offset(pageNumber.toLong() * pageSize)
            .limit(pageSize.toLong())
            .orderBy(stampEntity.createdAt.asc())
            .fetch()
            .map { it.toModel() }
    }

    override fun countExpiringStamps(
        principalKey: String,
        now: LocalDateTime,
        nDaysLater: LocalDateTime,
    ): Long {
        return jpaQueryFactory
            .select(stampEntity.count())
            .from(stampEntity)
            .where(
                stampEntity.principalKey.eq(principalKey),
                stampEntity.state.eq(StampState.EARNED),
                stampEntity.expiredAt.between(now, nDaysLater),
            )
            .fetchOne() ?: 0L
    }

    override fun findByOrderKey(orderKey: String): List<Stamp> {
        return jpaQueryFactory
            .selectFrom(stampEntity)
            .where(stampEntity.orderKey.eq(orderKey))
            .fetch()
            .map { it.toModel() }
    }
}
