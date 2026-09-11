package com.coffee.gu.payment

import org.springframework.stereotype.Component

@Component
class PaymentReader(
    private val paymentRepository: PaymentRepository,
) {
    fun getByOrderKey(orderKey: String): Payment {
        return paymentRepository.findByOrderKey(orderKey)
    }

    fun getByIdWithLock(paymentId: Long): Payment {
        return paymentRepository.findByIdWithLock(paymentId)
    }

    fun getByOrderKeyWithLock(orderKey: String): Payment {
        return paymentRepository.findByOrderKeyWithLock(orderKey)
    }

    fun getPendingPayments(limit: Int): List<Payment> {
        return paymentRepository.getPendingPayments(limit)
    }
}
