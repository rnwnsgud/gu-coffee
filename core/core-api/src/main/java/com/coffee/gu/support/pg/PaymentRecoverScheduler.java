package com.coffee.gu.support.pg;

import com.coffee.gu.PaymentGatewayConfirm;
import com.coffee.gu.order.Order;
import com.coffee.gu.order.OrderReader;
import com.coffee.gu.payment.Payment;
import com.coffee.gu.payment.PaymentCompleter;
import com.coffee.gu.payment.PaymentGatewayProcessor;
import com.coffee.gu.payment.PaymentReader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentRecoverScheduler {
    private final PaymentReader paymentReader;
    private final OrderReader orderReader;
    private final PaymentGatewayProcessor paymentGatewayProcessor;
    private final PaymentCompleter paymentCompleter;

    public PaymentRecoverScheduler(PaymentReader paymentReader, OrderReader orderReader, PaymentGatewayProcessor paymentGatewayProcessor, PaymentCompleter paymentCompleter) {
        this.paymentReader = paymentReader;
        this.orderReader = orderReader;
        this.paymentGatewayProcessor = paymentGatewayProcessor;
        this.paymentCompleter = paymentCompleter;
    }

    @Scheduled(fixedRate = 60000)
    public void schedule() {
        for (Payment pendingPayment : paymentReader.getPendingPayments()) {
            try {
                com.coffee.gu.PGConfirmResult pgConfirmResult = paymentGatewayProcessor.approvePayment(new PaymentGatewayConfirm(pendingPayment.getExternalPaymentKey(), pendingPayment.getOrderKey(), pendingPayment.getAmount()));
                Order order = orderReader.getByOrderKey(pendingPayment.getOrderKey());
                paymentCompleter.complete(order, pendingPayment.getId(), pgConfirmResult);
            } catch (Exception e) {
                // 한 건이 실패해도 다른 결제 건의 스케줄링에 영향을 주지 않도록 격리
                // 알림, dlq ...
            }
        }
    }
}
