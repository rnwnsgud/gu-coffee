package com.coffee.gu.cancel

import com.coffee.gu.payment.Payment
import org.springframework.stereotype.Component

@Component
class CancelManager(
    private val cancelRepository: CancelRepository,
) {
    fun cancel(payment: Payment, externalCancelKey: String): Cancel {
        return cancelRepository.cancel(payment, externalCancelKey)
    }
}
