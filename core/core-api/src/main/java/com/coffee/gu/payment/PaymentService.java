package com.coffee.gu.payment;


import com.coffee.gu.*;
import com.coffee.gu.order.Order;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentGatewayProcessor paymentGatewayProcessor;
    private final PaymentPreparer paymentPreparer;
    private final PaymentCompleter paymentCompleter;
    private final PaymentManager paymentManager;
    private final PaymentReader paymentReader;

    public PaymentService(PaymentGatewayProcessor paymentGatewayProcessor, PaymentPreparer paymentPreparer, PaymentCompleter paymentCompleter, PaymentManager paymentManager, PaymentReader paymentReader) {
        this.paymentGatewayProcessor = paymentGatewayProcessor;
        this.paymentPreparer = paymentPreparer;
        this.paymentCompleter = paymentCompleter;
        this.paymentManager = paymentManager;
        this.paymentReader = paymentReader;
    }

    public Long createPayment(Order order, PaymentDiscount paymentDiscount) {
        paymentManager.checkAlreadyPaid(order.getKey());
        return paymentManager.createPayment(order, paymentDiscount);
    }

    public PaymentApprovalResult approvePayment(Order order) {
        Payment payment = paymentReader.getByOrderKey(order.getKey());
        if (payment.isPaid()) return PaymentApprovalResult.alReadyApproved(order.getKey(), payment.getExternalPaymentKey(), payment.getPaidAt());
        PGPayment pgPayment = paymentGatewayProcessor.getPGPayment(order.getKey());
        payment = paymentPreparer.prepare(order, pgPayment);
        PGConfirmResult pgConfirmResult = confirm(pgPayment);
        return paymentCompleter.complete(order, payment.getId(), pgConfirmResult);
    }

    private PGConfirmResult confirm(PGPayment pgPayment) {
        try {
            return paymentGatewayProcessor.approvePayment(new PaymentGatewayConfirm(pgPayment.paymentKey(), pgPayment.orderKey(), pgPayment.amount()));
        } catch (Exception e) {
            throw new CoreException(ErrorType.PAYMENT_FAIL, null);
        }
    }

    public void fail(Order order, String code, String message) {
        Payment payment = paymentReader.getByOrderKey(order.getKey());
        paymentCompleter.failProcess(order, payment, code, message);
    }
}
