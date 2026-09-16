package com.coffee.gu.stamp

import com.coffee.gu.PaymentApprovedEvent
import com.coffee.gu.order.OrderReader
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentApprovedEventListener(
    private val stampHandler: StampHandler,
    private val orderReader: OrderReader,
) {
    private val log = LoggerFactory.getLogger(PaymentApprovedEventListener::class.java)

    @Async("stampAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentApproved(event: PaymentApprovedEvent) {
        if (event.hasAppliedCoupon) return
        try {
            val order = orderReader.getByOrderKey(event.orderKey)
            stampHandler.reward(order)
            log.info("스탬프 적립 비동기 처리 완료: orderKey={}", event.orderKey)
        } catch (e: Exception) {
            log.error("스탬프 적립 비동기 처리 중 오류 발생 : orderKey={}, error={}", event.orderKey, e.message, e)
        }
    }
}
