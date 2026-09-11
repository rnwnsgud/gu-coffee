package com.coffee.gu.payment

import com.coffee.gu.PGConfirmResult
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.order.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Component
class PaymentManager(
    private val paymentRepository: PaymentRepository,
) {
    fun createPayment(order: Order, paymentDiscount: PaymentDiscount): Long {
        val payment = paymentRepository.save(Payment.create(order, paymentDiscount))
        return payment.id
    }

    @Transactional
    fun pay(payment: Payment, result: PGConfirmResult) {
        payment.success(
            result.paymentKey,
            result.paymentMethod ?: PaymentMethod.CARD,
            result.approveCode,
            result.approvedAt ?: OffsetDateTime.now()
        )
        paymentRepository.save(payment)
    }

    fun prepare(payment: Payment): Payment {
        payment.prepare()
        return paymentRepository.save(payment)
    }

    fun save(payment: Payment): Payment {
        return paymentRepository.save(payment)
    }
}
