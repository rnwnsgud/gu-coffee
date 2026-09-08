package com.coffee.gu.cancel

import com.coffee.gu.BaseEntity
import com.coffee.gu.Principal
import com.coffee.gu.enums.PrincipalType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "cancel")
class CancelEntity @JvmOverloads constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val principalKey: String,
    @Enumerated(EnumType.STRING)
    val principalType: PrincipalType,
    val orderKey: String,
    val paymentId: Long,
    val originalAmount: BigDecimal,
    val issuedCouponId: Long? = null,
    val couponDiscount: BigDecimal? = null,
    val paidAmount: BigDecimal,
    val canceledAmount: BigDecimal,
    val externalCancelKey: String? = null,
    val canceledAt: OffsetDateTime? = null,
) : BaseEntity() {

    fun toModel(): Cancel {
        return Cancel(
            id = id,
            principal = Principal(principalKey, principalType),
            orderKey = orderKey,
            paymentId = paymentId,
            originalAmount = originalAmount,
            issuedCouponId = issuedCouponId,
            couponDiscount = couponDiscount,
            paidAmount = paidAmount,
            canceledAmount = canceledAmount,
            externalCancelKey = externalCancelKey,
            canceledAt = canceledAt,
        )
    }

    companion object {
        @JvmStatic
        fun from(cancel: Cancel): CancelEntity = CancelEntity(
            id = cancel.id,
            principalKey = cancel.principal.key,
            principalType = cancel.principal.type,
            orderKey = cancel.orderKey,
            paymentId = cancel.paymentId,
            originalAmount = cancel.originalAmount,
            issuedCouponId = cancel.issuedCouponId,
            couponDiscount = cancel.couponDiscount,
            paidAmount = cancel.paidAmount,
            canceledAmount = cancel.canceledAmount,
            externalCancelKey = cancel.externalCancelKey,
            canceledAt = cancel.canceledAt,
        )
    }
}
