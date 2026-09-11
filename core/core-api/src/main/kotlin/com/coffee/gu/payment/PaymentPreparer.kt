package com.coffee.gu.payment

import com.coffee.gu.PGPayment
import com.coffee.gu.order.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentPreparer(
    private val paymentReader: PaymentReader,
    private val paymentValidator: PaymentValidator,
    private val paymentManager: PaymentManager,
) {
    @Transactional
    fun prepare(order: Order, pgPayment: PGPayment): Payment {
        val payment = paymentReader.getByOrderKeyWithLock(order.key)
        paymentValidator.validate(payment, order, pgPayment.amount)
        return paymentManager.prepare(payment)
    }
}
