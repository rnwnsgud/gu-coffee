package com.coffee.gu.payment;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.PGConfirmResult;
import com.coffee.gu.order.Order;
import com.coffee.gu.enums.PaymentState;

import com.coffee.gu.order.OrderLine;
import com.coffee.gu.stamp.StampHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
public class PaymentManager {
    private final PaymentRepository paymentRepository;
    private final StampHandler stampHandler;

    public PaymentManager(PaymentRepository paymentRepository, StampHandler stampHandler){
        this.paymentRepository = paymentRepository;
        this.stampHandler = stampHandler;
    }

    public void checkAlreadyPaid(String orderKey) {
        paymentRepository.findByOrderKey(orderKey).ifPresent(payment -> {
            if (payment.getState() == PaymentState.SUCCESS) {
                throw new CoreException(ErrorType.ORDER_ALREADY_PAID, null);
            }
        });
    }

    public Long createPayment(Order order, PaymentDiscount paymentDiscount) {
        Payment payment = paymentRepository.save(Payment.create(order, paymentDiscount));
        if (!paymentDiscount.hasUsedCoupon()) {
            long stampEligibleQuantity = order.getLines().stream()
                    .filter(OrderLine::getIsStampEligible)
                    .mapToLong(OrderLine::getQuantity)
                    .sum();
            if (stampEligibleQuantity > 0) {
                stampHandler.reward(order.getPrincipal(), order.getKey(), order.getStoreId(), stampEligibleQuantity);
            }
        }
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

}
