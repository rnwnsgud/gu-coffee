package com.coffee.gu

import com.coffee.gu.enums.EventType
import com.coffee.gu.enums.PrincipalType
import io.hypersistence.tsid.TSID

class LimitedCouponIssueEvent(
    val principal: Principal = Principal("", PrincipalType.USER),
    val limitedCouponId: Long = 0L,
    eventId: String = TSID.Factory.getTsid().toString(),
) : Event(EventType.COUPON_ISSUE, eventId)
