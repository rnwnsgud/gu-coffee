package com.coffee.gu.payment;

import com.coffee.gu.*;
import com.coffee.gu.coupon.IssuedCouponManager;
import com.coffee.gu.order.Order;
import com.coffee.gu.order.OrderManager;
import com.coffee.gu.enums.TransactionType;
import com.coffee.gu.PGConfirmResult;
import com.coffee.gu.stamp.StampHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
public class PaymentCompleter {

    private final PaymentReader paymentReader;
    private final PaymentManager paymentManager;
    private final OrderManager orderManager;
    private final IssuedCouponManager issuedCouponManager;
    private final StampHandler stampHandler;
    private final TransactionHistoryManager transactionHistoryManager;

    public PaymentCompleter(PaymentReader paymentReader, PaymentManager paymentManager, OrderManager orderManager, IssuedCouponManager issuedCouponManager, TransactionHistoryManager transactionHistoryManager, StampHandler stampHandler) {
        this.paymentReader = paymentReader;
        this.paymentManager = paymentManager;
        this.orderManager = orderManager;
        this.issuedCouponManager = issuedCouponManager;
        this.transactionHistoryManager = transactionHistoryManager;
        this.stampHandler = stampHandler;
    }

    @Transactional
    public PaymentApprovalResult complete(Order order, Long paymentId, PGConfirmResult confirmedPayment) {
        Payment payment = paymentReader.getByIdWithLock(paymentId);
        if (payment.isPaid()) return PaymentApprovalResult.alreadyApproved(order.getKey(), payment.getExternalPaymentKey(), payment.getPaidAt());
        if (payment.isFailed()) return PaymentApprovalResult.failed(order.getKey(), payment.getExternalPaymentKey(), payment.getPaidAt());
        if (!confirmedPayment.isConfirmed()) {
            payment.fail();
            paymentManager.save(payment);
            return PaymentApprovalResult.failed(order.getKey(), payment.getExternalPaymentKey(), payment.getPaidAt());
        }
        paymentManager.pay(payment, confirmedPayment);
        orderManager.pay(order);
        issuedCouponManager.use(payment);
        if (!payment.hasAppliedCoupon()) stampHandler.reward(order);
        transactionHistoryManager.record(TransactionType.PAYMENT, order, payment, "Payment processed", payment.getPaidAt());
        return PaymentApprovalResult.approved(order.getKey(), payment.getExternalPaymentKey(), payment.getPaidAt());
    }

    @Transactional
    public void failProcess(Order order, Payment payment, String code, String message) {
        payment.fail();
        paymentManager.save(payment);
        transactionHistoryManager.record(TransactionType.PAYMENT_FAIL, order, payment, String.format("%s %s", code, message), OffsetDateTime.now());
    }
}
