package com.coffee.gu.coupon

import com.coffee.gu.BaseEntity
import com.coffee.gu.Principal
import com.coffee.gu.enums.IssuedCouponState
import com.coffee.gu.enums.PrincipalType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "issued_coupon")
@Entity
class IssuedCouponEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val principalKey: String,
    @Enumerated(EnumType.STRING)
    val principalType: PrincipalType,
    val couponId: Long,
    @Enumerated(EnumType.STRING)
    val state: IssuedCouponState,
) : BaseEntity() {

    fun toModel(coupon: Coupon): IssuedCoupon {
        return IssuedCoupon(
            id = id,
            principal = Principal(principalKey, principalType),
            state = state,
            coupon = coupon,
        )
    }

    companion object {
        fun from(issuedCoupon: IssuedCoupon): IssuedCouponEntity = IssuedCouponEntity(
            id = issuedCoupon.id,
            principalKey = issuedCoupon.principal.key,
            principalType = issuedCoupon.principal.type,
            couponId = issuedCoupon.coupon.id ?: 0L,
            state = issuedCoupon.state,
        )
    }
}
