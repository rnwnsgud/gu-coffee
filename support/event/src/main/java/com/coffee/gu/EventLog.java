package com.coffee.gu;

import com.coffee.gu.enums.EventLogTarget;
import com.coffee.gu.enums.EventType;

import java.time.LocalDateTime;

public class EventLog {
    private String eventId;
    private EventType eventType;
    private EventLogTarget eventLogTarget;
    private String payload;
    private Boolean isPublished;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    public EventLog(String eventId, EventType eventType, EventLogTarget eventLogTarget, String payload, Boolean isPublished, LocalDateTime createdAt, LocalDateTime publishedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventLogTarget = eventLogTarget;
        this.payload = payload;
        this.isPublished = isPublished;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public String getPayload() {
        return payload;
    }
}
