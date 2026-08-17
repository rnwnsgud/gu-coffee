package com.coffee.gu.cancel;

import com.coffee.gu.CancelEvent;
import com.coffee.gu.EventDispatcher;
import com.coffee.gu.enums.EventType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class CancelEventDispatcher implements EventDispatcher {

    private final JsonMapper jsonMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public CancelEventDispatcher(JsonMapper jsonMapper, ApplicationEventPublisher applicationEventPublisher) {
        this.jsonMapper = jsonMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public boolean supports(EventType eventType) {
        return eventType == EventType.CANCEL;
    }

    @Override
    public void dispatch(String payload) {
        CancelEvent cancelEvent = jsonMapper.readValue(payload, CancelEvent.class);
        applicationEventPublisher.publishEvent(cancelEvent);
    }
}
