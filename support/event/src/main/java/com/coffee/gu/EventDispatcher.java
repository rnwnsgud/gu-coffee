package com.coffee.gu;

import com.coffee.gu.enums.EventType;

public interface EventDispatcher {
    boolean supports(EventType eventType);
    void dispatch(String payload);
}
