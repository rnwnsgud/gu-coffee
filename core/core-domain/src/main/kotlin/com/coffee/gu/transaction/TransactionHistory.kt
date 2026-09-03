package com.coffee.gu.transaction

import com.coffee.gu.Principal
import com.coffee.gu.enums.TransactionType
import com.coffee.gu.order.Order
import com.coffee.gu.payment.Payment
import java.math.BigDecimal
import java.time.OffsetDateTime

class TransactionHistory(
    val id: Long = 0,
    val type: TransactionType,
    val principal: Principal,
    val orderKey: String,
    val paymentId: Long,
    val externalTransactionKey: String? = null,
    val amount: BigDecimal,
    val message: String? = null,
    val occurredAt: OffsetDateTime? = null,
) {
    companion object {
        @JvmStatic
        fun create(
            type: TransactionType,
            order: Order,
            payment: Payment,
            message: String?,
            occurredAt: OffsetDateTime?,
        ): TransactionHistory {
            return TransactionHistory(
                id = 0,
                type = type,
                principal = order.principal,
                orderKey = order.key,
                paymentId = payment.id ?: 0L,
                externalTransactionKey = payment.externalPaymentKey,
                amount = payment.amount,
                message = message,
                occurredAt = occurredAt,
            )
        }
    }
}
