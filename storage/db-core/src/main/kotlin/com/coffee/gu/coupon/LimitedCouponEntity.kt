package com.coffee.gu.coupon

import com.coffee.gu.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "limited_coupon")
@Entity
class LimitedCouponEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,
    val totalQuantity: Int,
    var issuedQuantity: Int = 0,
) : BaseEntity() {

    fun toModel(coupon: Coupon): LimitedCoupon {
        return LimitedCoupon(
            id = id,
            coupon = coupon,
            totalQuantity = totalQuantity,
            issuedQuantity = issuedQuantity,
        )
    }

    companion object {
        fun from(limitedCoupon: LimitedCoupon): LimitedCouponEntity {
            return LimitedCouponEntity(
                id = limitedCoupon.id,
                couponId = limitedCoupon.coupon.id ?: 0L,
                totalQuantity = limitedCoupon.totalQuantity,
                issuedQuantity = limitedCoupon.issuedQuantity,
            )
        }
    }
}
