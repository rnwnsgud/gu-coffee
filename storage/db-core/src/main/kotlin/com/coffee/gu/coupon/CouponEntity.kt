package com.coffee.gu.coupon

import com.coffee.gu.BaseEntity
import com.coffee.gu.enums.CouponType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Table(name = "coupon")
@Entity
class CouponEntity @JvmOverloads constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val name: String,
    @Enumerated(EnumType.STRING)
    val type: CouponType,
    val discount: BigDecimal,
    val expiredAt: LocalDateTime,
) : BaseEntity() {

    fun toModel(): Coupon {
        return Coupon(
            id = id,
            name = name,
            type = type,
            discount = discount,
            expiredAt = expiredAt,
        )
    }
}
