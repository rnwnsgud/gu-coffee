package com.coffee.gu

import com.coffee.gu.enums.EventType
import io.hypersistence.tsid.TSID

class PaymentApprovedEvent(
    val orderKey: String,
    val hasAppliedCoupon: Boolean,
    eventId: String = TSID.Factory.getTsid().toString(),
) : Event(EventType.PAYMENT_APPROVED, eventId)

