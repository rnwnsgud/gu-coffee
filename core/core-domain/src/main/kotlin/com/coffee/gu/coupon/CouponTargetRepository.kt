package com.coffee.gu.coupon

import com.coffee.gu.enums.CouponTargetType

interface CouponTargetRepository {
    fun findAllByTargetTypeAndTargetIdIn(targetType: CouponTargetType, targetIds: List<Long>): List<CouponTarget>
}
