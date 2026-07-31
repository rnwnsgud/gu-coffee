package com.coffee.gu.payment;

import com.coffee.gu.PGPayment;
import com.coffee.gu.order.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentPreparer {

    private final PaymentReader paymentReader;
    private final PaymentValidator paymentValidator;
    private final PaymentManager paymentManager;

    public PaymentPreparer(PaymentReader paymentReader, PaymentValidator paymentValidator, PaymentManager paymentManager) {
        this.paymentReader = paymentReader;
        this.paymentValidator = paymentValidator;
        this.paymentManager = paymentManager;
    }

    @Transactional
    public Payment prepare(Order order, PGPayment pgPayment) {
        Payment payment = paymentReader.getByOrderKeyWithLock(order.getKey());
        paymentValidator.validate(payment, order, pgPayment.amount());
        return paymentManager.prepare(payment);
    }
}
