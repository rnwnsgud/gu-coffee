package com.coffee.gu.cancel

import com.coffee.gu.TransactionHistoryManager
import com.coffee.gu.enums.TransactionType
import com.coffee.gu.order.Order
import com.coffee.gu.payment.Payment
import org.springframework.stereotype.Component

@Component
class CancelRecorder(
    private val cancelManager: CancelManager,
    private val transactionHistoryManager: TransactionHistoryManager,
) {
    fun record(
        payment: Payment,
        order: Order,
        externalCancelKey: String = "PG_API_응답_취소_고유_값_저장",
        reason: String = "Cancel processed",
    ): Cancel {
        val cancel = cancelManager.cancel(payment, externalCancelKey)
        transactionHistoryManager.record(TransactionType.CANCEL, order, payment, reason, cancel.canceledAt)
        return cancel
    }
}
