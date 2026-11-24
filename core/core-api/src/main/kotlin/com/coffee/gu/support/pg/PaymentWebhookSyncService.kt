package com.coffee.gu.support.pg

import com.coffee.gu.PGConfirmResult
import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderReader
import com.coffee.gu.payment.Payment
import com.coffee.gu.payment.PaymentCompleter
import com.coffee.gu.payment.PaymentReader
import com.coffee.gu.payment.PaymentWebhookCommand
import com.coffee.gu.payment.PaymentWebhookSyncAction
import com.coffee.gu.payment.PaymentWebhookSyncPort
import com.coffee.gu.payment.PaymentWebhookSyncResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentWebhookSyncService(
    private val paymentReader: PaymentReader,
    private val orderReader: OrderReader,
    private val paymentCompleter: PaymentCompleter,
    private val paymentWebhookSynchronizer: PaymentWebhookSynchronizer,
) : PaymentWebhookSyncPort {

    @Transactional
    override fun syncStatus(command: PaymentWebhookCommand): PaymentWebhookSyncResult {
        val payment = paymentReader.getByOrderKeyWithLock(command.orderKey)
        val order = orderReader.getByOrderKey(command.orderKey)
        val normalizedStatus = command.status.trim().uppercase()

        return when (normalizedStatus) {
            "DONE" -> handleDone(order, payment, command)
            "CANCELED", "PARTIAL_CANCELED" -> handleCanceled(order, payment, command)
            "ABORTED", "EXPIRED" -> handleFailed(order, payment, command)
            else -> PaymentWebhookSyncResult(
                orderKey = order.key,
                paymentState = payment.state,
                orderState = order.state,
                action = PaymentWebhookSyncAction.IGNORED_ALREADY_PROCESSED,
            )
        }
    }

    private fun handleDone(
        order: Order,
        payment: Payment,
        command: PaymentWebhookCommand,
    ): PaymentWebhookSyncResult {
        if (payment.isPaid || payment.isCanceled || payment.isFailed || order.state == OrderState.CANCELED) {
            return PaymentWebhookSyncResult(
                orderKey = order.key,
                paymentState = payment.state,
                orderState = order.state,
                action = PaymentWebhookSyncAction.IGNORED_ALREADY_PROCESSED,
            )
        }

        paymentCompleter.complete(
            order = order,
            paymentId = payment.id,
            confirmedPayment = PGConfirmResult.success(
                orderId = order.key,
                paymentKey = command.paymentKey?.takeIf { it.isNotBlank() }
                    ?: payment.externalPaymentKey?.takeIf { it.isNotBlank() }
                    ?: order.key,
                paymentMethod = command.method ?: PaymentMethod.VIRTUAL_ACCOUNT,
                approveCode = command.approveCode,
                approvedAt = command.approvedAt,
            ),
        )
        return PaymentWebhookSyncResult(
            orderKey = order.key,
            paymentState = PaymentState.SUCCESS,
            orderState = order.state,
            action = PaymentWebhookSyncAction.PAID,
        )
    }

    private fun handleCanceled(
        order: Order,
        payment: Payment,
        command: PaymentWebhookCommand,
    ): PaymentWebhookSyncResult {
        if (payment.isCanceled || order.state == OrderState.CANCELED || !payment.isPaid) {
            return PaymentWebhookSyncResult(
                orderKey = order.key,
                paymentState = payment.state,
                orderState = order.state,
                action = PaymentWebhookSyncAction.IGNORED_ALREADY_PROCESSED,
            )
        }

        paymentWebhookSynchronizer.syncCanceled(order, payment, command)
        return PaymentWebhookSyncResult(
            orderKey = order.key,
            paymentState = payment.state,
            orderState = order.state,
            action = PaymentWebhookSyncAction.CANCELED,
        )
    }

    private fun handleFailed(
        order: Order,
        payment: Payment,
        command: PaymentWebhookCommand,
    ): PaymentWebhookSyncResult {
        if (payment.isFailed || payment.isCanceled || payment.isPaid || order.state == OrderState.CANCELED) {
            return PaymentWebhookSyncResult(
                orderKey = order.key,
                paymentState = payment.state,
                orderState = order.state,
                action = PaymentWebhookSyncAction.IGNORED_ALREADY_PROCESSED,
            )
        }

        paymentWebhookSynchronizer.syncFailed(order, payment, command)
        return PaymentWebhookSyncResult(
            orderKey = order.key,
            paymentState = payment.state,
            orderState = order.state,
            action = PaymentWebhookSyncAction.FAILED,
        )
    }
}
