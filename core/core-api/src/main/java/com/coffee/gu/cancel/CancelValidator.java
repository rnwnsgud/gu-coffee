package com.coffee.gu.cancel;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.order.Order;
import com.coffee.gu.payment.Payment;
import com.coffee.gu.enums.PaymentState;

import com.coffee.gu.stamp.StampRevertManager;
import org.springframework.stereotype.Component;


@Component
public class CancelValidator {

    private final StampRevertManager stampRevertManager;

    public CancelValidator(StampRevertManager stampRevertManager) {
        this.stampRevertManager = stampRevertManager;
    }

    public void validate(Order order, Payment payment) {
        if (payment.getState() != PaymentState.SUCCESS) throw new CoreException(ErrorType.PAYMENT_INVALID_STATE, null);
        stampRevertManager.validateRevertable(order);
    }
}
