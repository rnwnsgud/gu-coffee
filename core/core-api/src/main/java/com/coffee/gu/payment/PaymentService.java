package com.coffee.gu.payment;


import com.coffee.gu.*;
import com.coffee.gu.order.Order;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentGatewayProcessor paymentGatewayProcessor;
    private final PaymentPreparer paymentPreparer;
    private final PaymentCompleter paymentCompleter;
    private final PaymentManager paymentManager;
    private final PaymentReader paymentReader;
    private final EventLogRepository eventLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentService(PaymentGatewayProcessor paymentGatewayProcessor, PaymentPreparer paymentPreparer, PaymentCompleter paymentCompleter, PaymentManager paymentManager, PaymentReader paymentReader, EventLogRepository eventLogRepository, ApplicationEventPublisher eventPublisher) {
        this.paymentGatewayProcessor = paymentGatewayProcessor;
        this.paymentPreparer = paymentPreparer;
        this.paymentCompleter = paymentCompleter;
        this.paymentManager = paymentManager;
        this.paymentReader = paymentReader;
        this.eventLogRepository = eventLogRepository;
        this.eventPublisher = eventPublisher;
    }

    public Long createPayment(Order order, PaymentDiscount paymentDiscount) {
        return paymentManager.createPayment(order, paymentDiscount);
    }

    public PaymentApprovalResult approvePayment(Order order) {
        Payment payment = paymentReader.getByOrderKey(order.getKey());
        if (!payment.isReady()) return PaymentApprovalResult.fromExisting(payment);
        PGPayment pgPayment = paymentGatewayProcessor.getPGPayment(order.getKey());
        payment = paymentPreparer.prepare(order, pgPayment);
        PGConfirmResult pgConfirmResult = paymentGatewayProcessor.approvePayment(new PaymentGatewayConfirm(pgPayment.paymentKey(), pgPayment.orderKey(), pgPayment.amount()));
        try {
            return paymentCompleter.complete(order, payment.getId(), pgConfirmResult);
        } catch (Exception e) {
            CancelEvent event = new CancelEvent(order.getKey());
            eventLogRepository.saveIfNotExists(event);
            eventPublisher.publishEvent(event);
            return PaymentApprovalResult.failed(order.getKey(), payment.getExternalPaymentKey(), payment.getPaidAt());
        }
    }

    public void fail(Order order, String code, String message) {
        Payment payment = paymentReader.getByOrderKey(order.getKey());
        paymentCompleter.failProcess(order, payment, code, message);
    }
}
