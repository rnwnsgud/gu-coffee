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

    fun claimPendingPayments(limit: Int): List<Payment> {
        return paymentRepository.claimPendingPayments(limit)
    }
}
