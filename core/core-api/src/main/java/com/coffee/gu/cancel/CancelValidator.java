package com.coffee.gu.cancel;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.Principal;
import com.coffee.gu.order.Order;
import com.coffee.gu.payment.Payment;
import com.coffee.gu.enums.PaymentState;

import com.coffee.gu.stamp.StampHandler;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CancelValidator {

    private final StampHandler stampHandler;

    public CancelValidator(StampHandler stampHandler) {
        this.stampHandler = stampHandler;
    }

    public void validate(Order order, Payment payment, Principal principal) {
        if (!Objects.equals(order.getPrincipal(), principal)) {
            throw new CoreException(ErrorType.NOT_FOUND_DATA, null);
        }
        if (payment.getState() != PaymentState.SUCCESS) throw new CoreException(ErrorType.PAYMENT_INVALID_STATE, null);
        stampHandler.validateRevertable(order);
    }
}
