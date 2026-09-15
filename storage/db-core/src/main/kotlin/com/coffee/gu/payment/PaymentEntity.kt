package com.coffee.gu.payment

import com.coffee.gu.BaseEntity
import com.coffee.gu.Principal
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.enums.PrincipalType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime

@Table(
    name = "payment",
    indexes = [
        Index(name = "udx_order_key", columnList = "orderKey", unique = true),
    ],
)
@Entity
class PaymentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val principalKey: String,
    @Enumerated(EnumType.STRING)
    val principalType: PrincipalType,
    val orderKey: String,
    val originalAmount: BigDecimal,
    val issuedCouponId: Long? = null,
    val couponDiscount: BigDecimal? = null,
    val paidAmount: BigDecimal,
    @Enumerated(EnumType.STRING)
    var state: PaymentState,
    var externalPaymentKey: String? = null,
    @Enumerated(EnumType.STRING)
    var method: PaymentMethod? = null,
    var paidAt: OffsetDateTime? = null,
    var approveCode: String? = null,
    var retryCount: Int = 0,
) : BaseEntity() {

    fun toModel(): Payment {
        return Payment(
            id = id,
            principal = Principal(principalKey, principalType),
            orderKey = orderKey,
            originalAmount = originalAmount,
            issuedCouponId = issuedCouponId,
            couponDiscount = couponDiscount,
            amount = paidAmount,
            state = state,
            externalPaymentKey = externalPaymentKey,
            method = method,
            paidAt = paidAt,
            approveCode = approveCode,
            createdAt = createdAt,
            retryCount = retryCount,
        )
    }

    companion object {
        fun from(payment: Payment): PaymentEntity {
            return PaymentEntity(
                id = payment.id,
                principalKey = payment.principal.key,
                principalType = payment.principal.type,
                orderKey = payment.orderKey,
                originalAmount = payment.originalAmount,
                issuedCouponId = payment.issuedCouponId,
                couponDiscount = payment.couponDiscount,
                paidAmount = payment.amount,
                state = payment.state,
                externalPaymentKey = payment.externalPaymentKey,
                method = payment.method,
                paidAt = payment.paidAt,
                approveCode = payment.approveCode,
                retryCount = payment.retryCount,
            )
        }
    }
}
