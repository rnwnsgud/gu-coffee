package com.coffee.gu

import com.coffee.gu.enums.PaymentMethod
import java.time.OffsetDateTime

data class PGConfirmResult(
    val orderId: String,
    val paymentKey: String,
    val isConfirmed: Boolean,
    val paymentMethod: PaymentMethod? = null,
    val approveCode: String? = null,
    val approvedAt: OffsetDateTime? = null,
) {
    companion object {
        fun success(
            orderId: String,
            paymentKey: String,
            paymentMethod: PaymentMethod?,
            approveCode: String?,
            approvedAt: OffsetDateTime?,
        ): PGConfirmResult = PGConfirmResult(
            orderId = orderId,
            paymentKey = paymentKey,
            isConfirmed = true,
            paymentMethod = paymentMethod,
            approveCode = approveCode,
            approvedAt = approvedAt,
        )

        fun fail(
            orderId: String,
            paymentKey: String,
        ): PGConfirmResult = PGConfirmResult(
            orderId = orderId,
            paymentKey = paymentKey,
            isConfirmed = false,
        )
    }
}
