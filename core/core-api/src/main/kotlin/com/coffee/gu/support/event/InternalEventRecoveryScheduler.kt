package com.coffee.gu.support.event

import com.coffee.gu.EventDispatcher
import com.coffee.gu.EventLogRepository
import com.coffee.gu.EventRecoveryScheduler
import com.coffee.gu.enums.EventLogTarget
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class InternalEventRecoveryScheduler(
    private val eventLogRepository: EventLogRepository,
    private val eventDispatchers: List<EventDispatcher>,
) : EventRecoveryScheduler {

    companion object {
        private val log = LoggerFactory.getLogger(InternalEventRecoveryScheduler::class.java)
        private const val MAX_RETRY_LIMIT = 5
    }

    @Scheduled(fixedRate = 5000)
    override fun republish() {
        val eventLogs = eventLogRepository.getUnpublishedEventLogs(EventLogTarget.INTERNAL)
        for (eventLog in eventLogs) {
            if (eventLog.retryCount != null && eventLog.retryCount!! >= MAX_RETRY_LIMIT) {
                log.warn("Outbox 이벤트 재발행 5회 초과 실패로 DEAD 전환: eventId={}, eventType={}", eventLog.eventId, eventLog.eventType)
                eventLogRepository.markAsDead(eventLog.eventId)
                continue
            }

            try {
                eventDispatchers.filter { it.supports(eventLog.eventType) }
                    .forEach { it.dispatch(eventLog.payload) }

                eventLogRepository.republish(listOf(eventLog.eventId))
            } catch (e: Exception) {
                log.error("Outbox 이벤트 재발행 실패 (retryCount 증가): eventId={}", eventLog.eventId, e)
                eventLogRepository.increaseRetryCount(eventLog.eventId)
            }
        }
    }
}
