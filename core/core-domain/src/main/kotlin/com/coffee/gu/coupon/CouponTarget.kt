package com.coffee.gu.coupon

import com.coffee.gu.enums.CouponTargetType

class CouponTarget(
    val id: Long = 0,
    val couponId: Long,
    val targetType: CouponTargetType,
    val targetId: Long,
)
