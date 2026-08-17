package com.coffee.gu.event;

import com.coffee.gu.EventLog;
import com.coffee.gu.enums.EventLogStatus;
import com.coffee.gu.enums.EventLogTarget;
import com.coffee.gu.enums.EventType;
import com.coffee.gu.BaseCustomIdEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Table(name = "event_log")
@Entity
public class EventLogEntity extends BaseCustomIdEntity<String> {

    @Enumerated(EnumType.STRING)
    private EventType eventType;
    @Enumerated(EnumType.STRING)
    private EventLogTarget eventLogTarget;
    private String payload;
    private Boolean isPublished;
    
    @Enumerated(EnumType.STRING)
    private EventLogStatus status;
    private Integer retryCount;
    
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    protected EventLogEntity() {
        super();
    }

    private EventLogEntity(String eventId, EventType eventType, String payload, Boolean isPublished, EventLogStatus status, Integer retryCount, LocalDateTime createdAt, LocalDateTime publishedAt, boolean isNewEntity) {
        super(eventId, isNewEntity);
        this.eventType = eventType;
        this.payload = payload;
        this.isPublished = isPublished;
        this.status = status;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    public static EventLogEntity create(String eventId, EventType eventType, String payload) {
        return new EventLogEntity(
                eventId,
                eventType,
                payload,
                false,
                EventLogStatus.PENDING,
                0,
                LocalDateTime.now(),
                null,
                true);
    }

    public EventLog toModel() {
        return new EventLog(
                this.id,
                this.eventType,
                this.eventLogTarget,
                this.payload,
                this.isPublished,
                this.status,
                this.retryCount,
                this.createdAt,
                this.publishedAt);
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Boolean getPublished() {
        return isPublished;
    }

    public EventLogStatus getStatus() {
        return status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
}
