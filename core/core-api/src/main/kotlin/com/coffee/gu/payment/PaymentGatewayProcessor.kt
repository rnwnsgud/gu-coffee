package com.coffee.gu.payment

import com.coffee.gu.PGCancelResult
import com.coffee.gu.PGConfirmResult
import com.coffee.gu.PGPayment
import com.coffee.gu.PaymentGateway
import com.coffee.gu.PaymentGatewayCancel
import com.coffee.gu.PaymentGatewayConfirm
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component

@Component
class PaymentGatewayProcessor(
    private val paymentGateway: PaymentGateway,
) {
    fun getPGPayment(orderKey: String): PGPayment {
        return paymentGateway.getByOrderKey(orderKey)
    }

    @Retryable(
        includes = [Exception::class],
        maxRetries = 2,
        delay = 1000
    )
    fun approvePayment(request: PaymentGatewayConfirm): PGConfirmResult {
        return paymentGateway.confirm(request)
    }

    fun cancelPayment(request: PaymentGatewayCancel): PGCancelResult {
        return paymentGateway.cancel(request)
    }
}
