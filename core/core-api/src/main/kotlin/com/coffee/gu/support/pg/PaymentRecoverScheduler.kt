package com.coffee.gu.support.pg

import com.coffee.gu.PGConfirmResult
import com.coffee.gu.PaymentGatewayStatus
import com.coffee.gu.cancel.CancelService
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.order.OrderReader
import com.coffee.gu.payment.PaymentCompleter
import com.coffee.gu.payment.PaymentGatewayProcessor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.OffsetDateTime

@Component
class PaymentRecoverScheduler(
    private val paymentRecoveryProcessor: PaymentRecoveryProcessor,
    private val orderReader: OrderReader,
    private val paymentGatewayProcessor: PaymentGatewayProcessor,
    private val paymentCompleter: PaymentCompleter,
    private val cancelService: CancelService,
) {
    companion object {
        private const val MAX_RETRY_COUNT = 5
        const val LIMIT = 20
    }

    @Scheduled(cron = "0 * * * * *")
    fun schedule() {
        val pendingPayments = paymentRecoveryProcessor.claimPendingPayments(LIMIT)
        for (pendingPayment in pendingPayments) {
            val order = orderReader.getByOrderKey(pendingPayment.orderKey)

            if (pendingPayment.isExpired(Duration.ofMinutes(30))) {
                try {
                    cancelService.cancel(order)
                } catch (e: Exception) {
                    paymentRecoveryProcessor.handleExpireFail(pendingPayment)
                }
                continue
            }

            try {
                val pgPayment = paymentGatewayProcessor.getPGPayment(pendingPayment.orderKey)
                if (pgPayment.status == PaymentGatewayStatus.DONE) {
                    val pgConfirmResult = PGConfirmResult.success(
                        pgPayment.orderKey,
                        pgPayment.paymentKey,
                        pendingPayment.method ?: PaymentMethod.CARD,
                        pendingPayment.approveCode,
                        pendingPayment.paidAt ?: OffsetDateTime.now()
                    )
                    paymentCompleter.complete(order, pendingPayment.id, pgConfirmResult)
                } else {
                    cancelService.cancel(order)
                }
            } catch (e: Exception) {
                paymentRecoveryProcessor.handleRetry(pendingPayment, MAX_RETRY_COUNT)
            }
        }
    }
}
