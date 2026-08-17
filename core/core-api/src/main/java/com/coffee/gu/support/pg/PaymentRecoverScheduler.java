package com.coffee.gu.support.pg;

import com.coffee.gu.PGConfirmResult;
import com.coffee.gu.PGPayment;
import com.coffee.gu.PaymentGatewayStatus;
import com.coffee.gu.cancel.CancelService;
import com.coffee.gu.order.Order;
import com.coffee.gu.order.OrderReader;
import com.coffee.gu.payment.Payment;
import com.coffee.gu.payment.PaymentCompleter;
import com.coffee.gu.payment.PaymentGatewayProcessor;
import com.coffee.gu.payment.PaymentManager;
import com.coffee.gu.payment.PaymentReader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

@Component
public class PaymentRecoverScheduler {
    private static final int MAX_RETRY_COUNT = 5;
    public static final int LIMIT = 20;
    private final PaymentReader paymentReader;
    private final OrderReader orderReader;
    private final PaymentGatewayProcessor paymentGatewayProcessor;
    private final PaymentCompleter paymentCompleter;
    private final PaymentManager paymentManager;
    private final CancelService cancelService;

    public PaymentRecoverScheduler(PaymentReader paymentReader, OrderReader orderReader, PaymentGatewayProcessor paymentGatewayProcessor, PaymentCompleter paymentCompleter, PaymentManager paymentManager, CancelService cancelService) {
        this.paymentReader = paymentReader;
        this.orderReader = orderReader;
        this.paymentGatewayProcessor = paymentGatewayProcessor;
        this.paymentCompleter = paymentCompleter;
        this.paymentManager = paymentManager;
        this.cancelService = cancelService;
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void schedule() {
        for (Payment pendingPayment : paymentReader.getPendingPayments(LIMIT)) {
            Order order = orderReader.getByOrderKey(pendingPayment.getOrderKey());

            if (pendingPayment.isExpired(Duration.ofMinutes(30))) {
                try {
                    cancelService.cancel(order);
                } catch (Exception e) {
                    pendingPayment.fail();
                    paymentManager.save(pendingPayment);
                }
                continue;
            }

            try {
                PGPayment pgPayment = paymentGatewayProcessor.getPGPayment(pendingPayment.getOrderKey());
                if (pgPayment.status() == PaymentGatewayStatus.DONE) {
                    PGConfirmResult pgConfirmResult = PGConfirmResult.success(
                            pgPayment.orderKey(),
                            pgPayment.paymentKey(),
                            pendingPayment.getMethod(),
                            pendingPayment.getApproveCode(),
                            pendingPayment.getPaidAt() != null ? pendingPayment.getPaidAt() : OffsetDateTime.now()
                    );
                    paymentCompleter.complete(order, pendingPayment.getId(), pgConfirmResult);
                } else {
                    cancelService.cancel(order);
                }
            } catch (Exception e) {
                pendingPayment.increaseRetryCount();
                if (pendingPayment.isRetryLimitExceeded(MAX_RETRY_COUNT)) {
                    pendingPayment.fail();
                    // 알림
                }
                paymentManager.save(pendingPayment);
            }
        }
    }
}
