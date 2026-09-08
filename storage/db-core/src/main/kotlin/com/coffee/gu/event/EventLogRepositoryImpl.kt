package com.coffee.gu.event

import com.coffee.gu.Event
import com.coffee.gu.EventLog
import com.coffee.gu.EventLogRepository
import com.coffee.gu.enums.EventLogStatus
import com.coffee.gu.enums.EventLogTarget
import com.coffee.gu.event.QEventLogEntity.eventLogEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime

@Repository
class EventLogRepositoryImpl(
    private val eventLogJpaRepository: EventLogJpaRepository,
    private val jpaQueryFactory: JPAQueryFactory,
    private val jsonMapper: JsonMapper,
) : EventLogRepository {

    override fun saveIfNotExists(event: Event): Boolean {
        return try {
            val payload = jsonMapper.writeValueAsString(event)
            eventLogJpaRepository.save(EventLogEntity.create(event.eventId, event.eventType, payload))
            true
        } catch (e: DataIntegrityViolationException) {
            false
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    override fun publish(event: Event) {
        jpaQueryFactory
            .update(eventLogEntity)
            .set(eventLogEntity.isPublished, true)
            .set(eventLogEntity.status, EventLogStatus.SUCCESS)
            .set(eventLogEntity.publishedAt, LocalDateTime.now())
            .where(eventLogEntity.eventId.eq(event.eventId))
            .execute()
    }

    @Transactional
    override fun republish(eventIds: Collection<String>) {
        jpaQueryFactory
            .update(eventLogEntity)
            .set(eventLogEntity.isPublished, true)
            .set(eventLogEntity.status, EventLogStatus.SUCCESS)
            .set(eventLogEntity.publishedAt, LocalDateTime.now())
            .where(eventLogEntity.eventId.`in`(eventIds))
            .execute()
    }

    @Transactional
    override fun increaseRetryCount(eventId: String) {
        jpaQueryFactory
            .update(eventLogEntity)
            .set(eventLogEntity.retryCount, eventLogEntity.retryCount.add(1))
            .where(eventLogEntity.eventId.eq(eventId))
            .execute()
    }

    @Transactional
    override fun markAsDead(eventId: String) {
        jpaQueryFactory
            .update(eventLogEntity)
            .set(eventLogEntity.status, EventLogStatus.DEAD)
            .where(eventLogEntity.eventId.eq(eventId))
            .execute()
    }

    override fun getUnpublishedEventLogs(eventLogTarget: EventLogTarget): List<EventLog> {
        val createdBefore = LocalDateTime.now().minusMinutes(5)
        return jpaQueryFactory
            .selectFrom(eventLogEntity)
            .where(eventLogEntity.isPublished.isFalse)
            .where(eventLogEntity.status.ne(EventLogStatus.DEAD).or(eventLogEntity.status.isNull))
            .where(
                eventLogEntity.eventLogTarget.eq(eventLogTarget),
                eventLogEntity.createdAt.before(createdBefore),
            )
            .limit(1000)
            .orderBy(eventLogEntity.createdAt.asc())
            .fetch()
            .map { it.toModel() }
    }
}
