package com.coffee.gu.payment;

import com.coffee.gu.PGConfirmResult;
import com.coffee.gu.order.Order;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
public class PaymentManager {
    private final PaymentRepository paymentRepository;

    public PaymentManager(PaymentRepository paymentRepository){
        this.paymentRepository = paymentRepository;
    }

    public Long createPayment(Order order, PaymentDiscount paymentDiscount) {
        Payment payment = paymentRepository.save(Payment.create(order, paymentDiscount));
        return payment.getId();
    }

    @Transactional
    public void pay(Payment payment, PGConfirmResult result) {
        payment.success(
                result.paymentKey(),
                result.paymentMethod(),
                result.approveCode(),
                result.approvedAt()
        );

        paymentRepository.save(payment);
    }

    public Payment prepare(Payment payment) {
        payment.prepare();
        return paymentRepository.save(payment);
    }

    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }
}
