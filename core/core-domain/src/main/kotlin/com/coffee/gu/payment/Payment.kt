package com.coffee.gu.payment

import com.coffee.gu.Principal
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.order.Order
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime

class Payment @JvmOverloads constructor(
    val id: Long = 0,
    val principal: Principal,
    val orderKey: String,
    val originalAmount: BigDecimal,
    val issuedCouponId: Long? = null,
    val couponDiscount: BigDecimal? = null,
    val amount: BigDecimal,
    var state: PaymentState,
    var externalPaymentKey: String? = null,
    var method: PaymentMethod? = null,
    var paidAt: OffsetDateTime? = null,
    var approveCode: String? = null,
    val createdAt: LocalDateTime? = null,
    var retryCount: Int = 0,
) {
    val isPaid: Boolean
        get() = state == PaymentState.SUCCESS

    val isReady: Boolean
        get() = state == PaymentState.READY

    val isFailed: Boolean
        get() = state == PaymentState.FAILED

    val hasAppliedCoupon: Boolean
        get() = issuedCouponId != null && issuedCouponId > 0

    fun hasAppliedCoupon(): Boolean = hasAppliedCoupon

    fun success(
        externalPaymentKey: String,
        method: PaymentMethod,
        approveCode: String?,
        paidAt: OffsetDateTime,
    ) {
        this.state = PaymentState.SUCCESS
        this.externalPaymentKey = externalPaymentKey
        this.method = method
        this.approveCode = approveCode
        this.paidAt = paidAt
    }

    fun prepare() {
        this.state = PaymentState.PENDING_PG
    }

    @JvmOverloads
    fun fail(externalPaymentKey: String? = null) {
        this.state = PaymentState.FAILED
        if (!externalPaymentKey.isNullOrBlank()) {
            this.externalPaymentKey = externalPaymentKey
        }
    }

    fun increaseRetryCount() {
        this.retryCount++
    }

    fun isRetryLimitExceeded(maxLimit: Int): Boolean {
        return this.retryCount >= maxLimit
    }

    fun isExpired(timeout: Duration): Boolean {
        val created = createdAt ?: return false
        return LocalDateTime.now().isAfter(created.plus(timeout))
    }

    companion object {
        @JvmStatic
        fun create(order: Order, paymentDiscount: PaymentDiscount): Payment {
            return Payment(
                principal = order.principal,
                orderKey = order.key,
                originalAmount = order.totalPrice,
                issuedCouponId = paymentDiscount.useIssuedCouponId,
                couponDiscount = paymentDiscount.couponDiscount,
                amount = paymentDiscount.paidAmount,
                state = PaymentState.READY,
                externalPaymentKey = null,
                method = null,
                paidAt = null,
                approveCode = null,
                createdAt = null,
                retryCount = 0,
            )
        }
    }
}
