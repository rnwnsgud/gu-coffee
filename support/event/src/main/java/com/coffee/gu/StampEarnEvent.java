package com.coffee.gu;

import com.coffee.gu.enums.EventType;

public class StampEarnEvent extends Event{
    private Principal principal;
    private Long storeId;
    private String storeName;

    public StampEarnEvent(Principal principal, Long storeId, String storeName) {
        super(EventType.STAMP_EARN);
        this.principal = principal;
        this.storeId = storeId;
        this.storeName = storeName;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getStoreName() {
        return storeName;
    }

}
