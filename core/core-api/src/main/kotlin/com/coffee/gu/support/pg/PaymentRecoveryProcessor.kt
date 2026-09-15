package com.coffee.gu.support.pg

import com.coffee.gu.payment.Payment
import com.coffee.gu.payment.PaymentManager
import com.coffee.gu.payment.PaymentReader
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentRecoveryProcessor(
    private val paymentReader: PaymentReader,
    private val paymentManager: PaymentManager,
) {

    @Transactional
    fun claimPendingPayments(limit: Int): List<Payment> {
        return paymentReader.claimPendingPayments(limit)
    }

    fun handleRetry(pendingPayment: Payment, maxRetryCount: Int) {
        pendingPayment.increaseRetryCount()
        if (pendingPayment.isRetryLimitExceeded(maxRetryCount)) {
            pendingPayment.fail()
        }
        paymentManager.save(pendingPayment)
    }

    fun handleExpireFail(pendingPayment: Payment) {
        pendingPayment.fail()
        paymentManager.save(pendingPayment)
    }
}
