package com.coffee.gu.cancel

import com.coffee.gu.CancelEvent
import com.coffee.gu.EventLogRepository
import com.coffee.gu.order.Order
import com.coffee.gu.payment.Payment
import com.coffee.gu.payment.PaymentManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CancelTxHandler(
    private val paymentManager: PaymentManager,
    private val cancelRollbacker: CancelRollbacker,
    private val cancelRecorder: CancelRecorder,
    private val eventLogRepository: EventLogRepository,
) {
    @Transactional
    fun completeCancelTx(
        order: Order,
        payment: Payment,
        event: CancelEvent,
        externalCancelKey: String = "PG_API_응답_취소_고유_값_저장",
        reason: String = "Cancel processed",
    ) {
        payment.cancel()
        paymentManager.save(payment)
        cancelRollbacker.rollback(order, payment)
        cancelRecorder.record(payment, order, externalCancelKey, reason)
        eventLogRepository.publish(event)
    }
}
