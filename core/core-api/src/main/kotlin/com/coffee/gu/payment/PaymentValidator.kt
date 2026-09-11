package com.coffee.gu.payment

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.order.Order
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class PaymentValidator {
    fun validate(payment: Payment, order: Order, amount: BigDecimal) {
        validateOwner(payment, order)
        validatePrepareState(payment)
        validateAmount(payment, amount)
    }

    private fun validateOwner(payment: Payment, order: Order) {
        if (payment.principal.key != order.principal.key) throw CoreException(ErrorType.NOT_FOUND_DATA)
    }

    private fun validatePrepareState(payment: Payment) {
        val state = payment.state
        if (state == PaymentState.READY || state == PaymentState.FAILED) return
        if (state == PaymentState.PENDING_PG) throw CoreException(ErrorType.PAYMENT_INVALID_STATE, "이미 결제 승인 처리 중입니다.")
        if (state == PaymentState.SUCCESS) throw CoreException(ErrorType.ORDER_ALREADY_PAID)
        throw CoreException(ErrorType.PAYMENT_INVALID_STATE, "결제 승인 준비를 할 수 없는 상태입니다.")
    }

    private fun validateAmount(payment: Payment, amount: BigDecimal) {
        if (payment.amount != amount) throw CoreException(ErrorType.PAYMENT_AMOUNT_MISMATCH)
    }
}
