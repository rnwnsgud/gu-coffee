package com.coffee.gu.cancel

import com.coffee.gu.CancelEvent
import com.coffee.gu.PaymentGatewayCancel
import com.coffee.gu.order.Order
import com.coffee.gu.payment.PaymentGatewayProcessor
import com.coffee.gu.payment.PaymentReader
import org.springframework.stereotype.Service

@Service
class CancelService(
    private val validator: CancelValidator,
    private val paymentReader: PaymentReader,
    private val paymentGatewayProcessor: PaymentGatewayProcessor,
    private val cancelTxHandler: CancelTxHandler,
) {
    fun cancel(order: Order) {
        cancel(order, CancelEvent(order.key))
    }

    fun cancel(order: Order, event: CancelEvent) {
        val payment = paymentReader.getByOrderKey(order.key)
        validator.validate(order, payment)
        paymentGatewayProcessor.cancelPayment(PaymentGatewayCancel(payment.externalPaymentKey ?: "", "구매자 변심"))
        cancelTxHandler.completeCancelTx(order, payment, event)
    }
}
