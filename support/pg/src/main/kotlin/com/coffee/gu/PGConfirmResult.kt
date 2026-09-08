package com.coffee.gu

import com.coffee.gu.enums.PaymentMethod
import java.time.OffsetDateTime

@JvmRecord
data class PGConfirmResult(
    val orderId: String,
    val paymentKey: String,
    val isConfirmed: Boolean,
    val paymentMethod: PaymentMethod? = null,
    val approveCode: String? = null,
    val approvedAt: OffsetDateTime? = null,
) {
    companion object {
        @JvmStatic
        fun success(
            orderId: String,
            paymentKey: String,
            paymentMethod: PaymentMethod,
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

        @JvmStatic
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
