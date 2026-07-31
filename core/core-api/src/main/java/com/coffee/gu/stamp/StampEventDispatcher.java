package com.coffee.gu.stamp;

import com.coffee.gu.EventDispatcher;
import com.coffee.gu.enums.EventType;
import com.coffee.gu.StampEarnEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StampEventDispatcher implements EventDispatcher {

    private final JsonMapper jsonMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public StampEventDispatcher(JsonMapper jsonMapper, ApplicationEventPublisher applicationEventPublisher) {
        this.jsonMapper = jsonMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public boolean supports(EventType eventType) {
        return eventType == EventType.STAMP_EARN;
    }

    @Override
    public void dispatch(String payload) {
        StampEarnEvent stampEarnEvent = jsonMapper.readValue(payload, StampEarnEvent.class);
        applicationEventPublisher.publishEvent(stampEarnEvent);
    }
}
