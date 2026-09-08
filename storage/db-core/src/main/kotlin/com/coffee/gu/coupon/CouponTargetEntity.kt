package com.coffee.gu.coupon

import com.coffee.gu.BaseEntity
import com.coffee.gu.enums.CouponTargetType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "coupon_target")
@Entity
class CouponTargetEntity @JvmOverloads constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val couponId: Long,
    @Enumerated(EnumType.STRING)
    val targetType: CouponTargetType,
    val targetId: Long,
) : BaseEntity() {

    fun toModel(): CouponTarget {
        return CouponTarget(
            id = id,
            couponId = couponId,
            targetType = targetType,
            targetId = targetId,
        )
    }
}
