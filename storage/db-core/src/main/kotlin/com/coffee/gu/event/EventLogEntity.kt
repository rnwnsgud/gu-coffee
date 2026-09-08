package com.coffee.gu.event

import com.coffee.gu.BaseCustomIdEntity
import com.coffee.gu.EventLog
import com.coffee.gu.enums.EventLogStatus
import com.coffee.gu.enums.EventLogTarget
import com.coffee.gu.enums.EventType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Table(name = "event_log")
@Entity
class EventLogEntity(
    @Id
    val eventId: String,
    @Enumerated(EnumType.STRING)
    val eventType: EventType,
    @Enumerated(EnumType.STRING)
    val eventLogTarget: EventLogTarget = EventLogTarget.INTERNAL,
    val payload: String,
    var isPublished: Boolean = false,
    @Enumerated(EnumType.STRING)
    var status: EventLogStatus = EventLogStatus.PENDING,
    var retryCount: Int = 0,
    var publishedAt: LocalDateTime? = null,
    isNewEntity: Boolean = true,
) : BaseCustomIdEntity<String>(isNewEntity) {

    override fun getId(): String = eventId

    fun toModel(): EventLog {
        return EventLog(
            eventId = eventId,
            eventType = eventType,
            eventLogTarget = eventLogTarget,
            payload = payload,
            isPublished = isPublished,
            status = status,
            retryCount = retryCount,
            createdAt = createdAt,
            publishedAt = publishedAt,
        )
    }

    companion object {
        @JvmStatic
        fun create(
            eventId: String,
            eventType: EventType,
            payload: String,
            eventLogTarget: EventLogTarget = EventLogTarget.INTERNAL,
        ): EventLogEntity {
            return EventLogEntity(
                eventId = eventId,
                eventType = eventType,
                eventLogTarget = eventLogTarget,
                payload = payload,
                isPublished = false,
                status = EventLogStatus.PENDING,
                retryCount = 0,
                publishedAt = null,
                isNewEntity = true,
            )
        }
    }
}
