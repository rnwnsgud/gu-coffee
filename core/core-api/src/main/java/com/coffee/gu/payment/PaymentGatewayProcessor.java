package com.coffee.gu.payment;

import com.coffee.gu.*;

import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayProcessor {

    private final PaymentGateway paymentGateway;

    public PaymentGatewayProcessor(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public PGPayment getPGPayment(String orderKey) {
        return paymentGateway.getByOrderKey(orderKey);
    }

    @Retryable(
            includes = Exception.class,
            maxRetries = 2,
            delay = 1000
    )
    public PGConfirmResult approvePayment(PaymentGatewayConfirm request) {
        return paymentGateway.confirm(request);
    }

    public PGCancelResult cancelPayment(PaymentGatewayCancel request) {
        return paymentGateway.cancel(request);
    }

}
