package com.coffee.gu

import com.coffee.gu.enums.TransactionType
import com.coffee.gu.order.Order
import com.coffee.gu.payment.Payment
import com.coffee.gu.transaction.TransactionHistory
import com.coffee.gu.transaction.TransactionHistoryRepository
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class TransactionHistoryManager(
    private val transactionHistoryRepository: TransactionHistoryRepository,
) {
    fun record(
        type: TransactionType,
        order: Order,
        payment: Payment,
        message: String,
        occurredAt: OffsetDateTime?
    ) {
        transactionHistoryRepository.save(TransactionHistory.create(type, order, payment, message, occurredAt))
    }
}
