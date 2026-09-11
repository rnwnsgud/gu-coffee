package com.coffee.gu.cancel

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.order.Order
import com.coffee.gu.payment.Payment
import com.coffee.gu.stamp.StampRevertManager
import org.springframework.stereotype.Component

@Component
class CancelValidator(
    private val stampRevertManager: StampRevertManager,
) {
    fun validate(order: Order, payment: Payment) {
        if (payment.state != PaymentState.SUCCESS) throw CoreException(ErrorType.PAYMENT_INVALID_STATE)
        stampRevertManager.validateRevertable(order)
    }
}
