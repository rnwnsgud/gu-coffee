package com.coffee.gu;

import com.coffee.gu.enums.EventType;
import io.hypersistence.tsid.TSID;

public abstract class Event {
    private String eventId;
    private EventType eventType;

    public Event(EventType eventType) {
        this.eventId = TSID.Factory.getTsid().toString();
        this.eventType = eventType;
    }

    public String getEventId() {
        return eventId;
    }
    public EventType getEventType() {
        return eventType;
    }

}
