package com.coffee.gu.cancel

import com.coffee.gu.Principal
import java.math.BigDecimal
import java.time.OffsetDateTime

class Cancel(
    val id: Long = 0,
    val principal: Principal,
    val orderKey: String,
    val paymentId: Long,
    val originalAmount: BigDecimal,
    val issuedCouponId: Long? = null,
    val couponDiscount: BigDecimal? = null,
    val paidAmount: BigDecimal,
    val canceledAmount: BigDecimal,
    val externalCancelKey: String? = null,
    val canceledAt: OffsetDateTime? = null,
)
