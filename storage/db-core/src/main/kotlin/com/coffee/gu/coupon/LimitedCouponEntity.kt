package com.coffee.gu.coupon

import com.coffee.gu.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Table(name = "limited_coupon")
@Entity
class LimitedCouponEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    val coupon: CouponEntity,
    val totalQuantity: Int,
    var issuedQuantity: Int = 0,
) : BaseEntity() {

    fun toModel(): LimitedCoupon {
        return LimitedCoupon(
            id = id,
            coupon = coupon.toModel(),
            totalQuantity = totalQuantity,
            issuedQuantity = issuedQuantity,
        )
    }

    companion object {
        fun from(limitedCoupon: LimitedCoupon, couponEntity: CouponEntity): LimitedCouponEntity {
            return LimitedCouponEntity(
                id = limitedCoupon.id,
                coupon = couponEntity,
                totalQuantity = limitedCoupon.totalQuantity,
                issuedQuantity = limitedCoupon.issuedQuantity,
            )
        }
    }
}
