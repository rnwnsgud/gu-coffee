package com.coffee.gu;

import com.coffee.gu.enums.EventType;

public class CancelEvent extends Event{

    private String orderKey;

    public CancelEvent(String orderKey) {
        super(EventType.CANCEL);
        this.orderKey = orderKey;
    }

    public String getOrderKey() {
        return orderKey;
    }
}
