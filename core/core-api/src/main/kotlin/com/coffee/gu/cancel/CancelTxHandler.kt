package com.coffee.gu.cancel

import com.coffee.gu.CancelEvent
import com.coffee.gu.EventLogRepository
import com.coffee.gu.order.Order
import com.coffee.gu.payment.Payment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CancelTxHandler(
    private val cancelRollbacker: CancelRollbacker,
    private val cancelRecorder: CancelRecorder,
    private val eventLogRepository: EventLogRepository,
) {
    @Transactional
    fun completeCancelTx(order: Order, payment: Payment, event: CancelEvent) {
        cancelRollbacker.rollback(order, payment)
        cancelRecorder.record(payment, order)
        eventLogRepository.publish(event)
    }
}
