package com.coffee.gu.support.pg

import com.coffee.gu.PGConfirmResult
import com.coffee.gu.PaymentGatewayStatus
import com.coffee.gu.cancel.CancelService
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.order.OrderReader
import com.coffee.gu.payment.PaymentCompleter
import com.coffee.gu.payment.PaymentGatewayProcessor
import com.coffee.gu.payment.PaymentManager
import com.coffee.gu.payment.PaymentReader
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.OffsetDateTime

@Component
class PaymentRecoverScheduler(
    private val paymentReader: PaymentReader,
    private val orderReader: OrderReader,
    private val paymentGatewayProcessor: PaymentGatewayProcessor,
    private val paymentCompleter: PaymentCompleter,
    private val paymentManager: PaymentManager,
    private val cancelService: CancelService,
) {
    companion object {
        private const val MAX_RETRY_COUNT = 5
        const val LIMIT = 20
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    fun schedule() {
        for (pendingPayment in paymentReader.getPendingPayments(LIMIT)) {
            val order = orderReader.getByOrderKey(pendingPayment.orderKey)

            if (pendingPayment.isExpired(Duration.ofMinutes(30))) {
                try {
                    cancelService.cancel(order)
                } catch (e: Exception) {
                    pendingPayment.fail()
                    paymentManager.save(pendingPayment)
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
                pendingPayment.increaseRetryCount()
                if (pendingPayment.isRetryLimitExceeded(MAX_RETRY_COUNT)) {
                    pendingPayment.fail()
                }
                paymentManager.save(pendingPayment)
            }
        }
    }
}
