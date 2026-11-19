package com.coffee.gu.support.pg

import com.coffee.gu.CancelEvent
import com.coffee.gu.TransactionHistoryManager
import com.coffee.gu.cancel.CancelTxHandler
import com.coffee.gu.enums.TransactionType
import com.coffee.gu.menu.LimitedMenuStockManager
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderManager
import com.coffee.gu.payment.Payment
import com.coffee.gu.payment.PaymentManager
import com.coffee.gu.payment.PaymentWebhookCommand
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class PaymentWebhookSynchronizer(
    private val paymentManager: PaymentManager,
    private val orderManager: OrderManager,
    private val cancelTxHandler: CancelTxHandler,
    private val transactionHistoryManager: TransactionHistoryManager,
    private val limitedMenuStockManager: LimitedMenuStockManager,
) {
    fun syncCanceled(
        order: Order,
        payment: Payment,
        command: PaymentWebhookCommand,
    ) {
        val cancelKey = command.cancelTransactionKey?.takeIf { it.isNotBlank() }
            ?: command.paymentKey?.takeIf { it.isNotBlank() }
            ?: payment.externalPaymentKey?.takeIf { it.isNotBlank() }
            ?: order.key
        val reason = command.cancelReason?.takeIf { it.isNotBlank() } ?: "PG Webhook Canceled"
        cancelTxHandler.completeCancelTx(
            order = order,
            payment = payment,
            event = CancelEvent(order.key),
            externalCancelKey = cancelKey,
            reason = reason,
        )
    }

    fun syncFailed(
        order: Order,
        payment: Payment,
        command: PaymentWebhookCommand,
    ) {
        payment.fail(command.paymentKey)
        paymentManager.save(payment)
        orderManager.cancel(order)
        limitedMenuStockManager.releaseIfLimited(
            orderKey = order.key,
            principalKey = order.principal.key,
            menuQuantities = order.toMenuQuantityMap(),
            restoreDbSoldQuantity = false,
        )
        val reason = command.cancelReason?.takeIf { it.isNotBlank() } ?: "PG Webhook ${command.status}"
        transactionHistoryManager.record(TransactionType.PAYMENT_FAIL, order, payment, reason, OffsetDateTime.now())
    }
}
