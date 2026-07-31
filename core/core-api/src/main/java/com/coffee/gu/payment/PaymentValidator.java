package com.coffee.gu.payment;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.order.Order;
import com.coffee.gu.enums.PaymentState;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class PaymentValidator {

    public void validate(Payment payment, Order order, BigDecimal amount) {
        validateOwner(payment, order);
        validatePrepareState(payment);
        validateAmount(payment, amount);
    }

    private void validateOwner(Payment payment, Order order) {
        if (!Objects.equals(payment.getPrincipal().getKey(), order.getPrincipal().getKey())) {
            throw new CoreException(ErrorType.NOT_FOUND_DATA, null);
        }
    }

    private void validatePrepareState(Payment payment) {
        PaymentState state = payment.getState();
        if (state == PaymentState.READY || state == PaymentState.FAILED) return;
        if (state == PaymentState.PENDING_PG) throw new CoreException(ErrorType.PAYMENT_INVALID_STATE, "이미 결제 승인 처리 중입니다.");
        if (state == PaymentState.SUCCESS) throw new CoreException(ErrorType.ORDER_ALREADY_PAID, null);
        throw new CoreException(ErrorType.PAYMENT_INVALID_STATE, "결제 승인 준비를 할 수 없는 상태입니다.");
    }

    private void validateAmount(Payment payment, BigDecimal amount) {
        if (!Objects.equals(payment.getAmount(), amount)) {
            throw new CoreException(ErrorType.PAYMENT_AMOUNT_MISMATCH, null);
        }
    }
}
