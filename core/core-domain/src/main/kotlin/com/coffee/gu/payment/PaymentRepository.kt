package com.coffee.gu.payment

interface PaymentRepository {
    fun findByOrderKey(orderKey: String): Payment
    fun save(payment: Payment): Payment
    fun findByIdWithLock(id: Long): Payment
    fun findByOrderKeyWithLock(orderKey: String): Payment
    fun claimPendingPayments(limit: Int): List<Payment>
}
