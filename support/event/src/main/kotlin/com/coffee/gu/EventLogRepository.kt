package com.coffee.gu

import com.coffee.gu.enums.EventLogTarget

interface EventLogRepository {
    fun saveIfNotExists(event: Event): Boolean

    /**
     * 반드시 기존 비즈니스 트랜잭션 내부에서 호출되어야 합니다. @Transactional(propagation = Propagation.MANDATORY)
     */
    fun publish(event: Event)
    fun republish(eventIds: Collection<String>)
    fun increaseRetryCount(eventId: String)
    fun markAsDead(eventId: String)
    fun getUnpublishedEventLogs(eventLogTarget: EventLogTarget): List<EventLog>
}
