package com.coffee.gu.stamp

import com.coffee.gu.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Table(name = "stamp_coupon_usage")
@Entity
class StampCouponUsageEntity @JvmOverloads constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val stampId: Long,
    val issuedCouponId: Long,
    val usedAt: LocalDateTime,
) : BaseEntity() {

    fun toModel(): StampCouponUsage {
        return StampCouponUsage(
            id = id,
            stampId = stampId,
            issuedCouponId = issuedCouponId,
            usedAt = usedAt,
        )
    }

    companion object {
        @JvmStatic
        fun from(stampCouponUsage: StampCouponUsage): StampCouponUsageEntity = StampCouponUsageEntity(
            id = stampCouponUsage.id ?: 0L,
            stampId = stampCouponUsage.stampId ?: 0L,
            issuedCouponId = stampCouponUsage.issuedCouponId ?: 0L,
            usedAt = stampCouponUsage.usedAt,
        )
    }
}
