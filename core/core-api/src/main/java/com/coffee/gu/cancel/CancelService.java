package com.coffee.gu.cancel;

import com.coffee.gu.CancelEvent;
import com.coffee.gu.PaymentGatewayCancel;
import com.coffee.gu.order.Order;
import com.coffee.gu.payment.Payment;
import com.coffee.gu.payment.PaymentGatewayProcessor;
import com.coffee.gu.payment.PaymentReader;
import org.springframework.stereotype.Service;

@Service
public class CancelService {

    private final CancelValidator validator;
    private final PaymentReader paymentReader;
    private final PaymentGatewayProcessor paymentGatewayProcessor;
    private final CancelTxHandler cancelTxHandler;

    public CancelService(CancelValidator validator, PaymentReader paymentReader, PaymentGatewayProcessor paymentGatewayProcessor, CancelTxHandler cancelTxHandler) {
        this.validator = validator;
        this.paymentReader = paymentReader;
        this.paymentGatewayProcessor = paymentGatewayProcessor;
        this.cancelTxHandler = cancelTxHandler;
    }

    public void cancel(Order order) {
        cancel(order, new CancelEvent(order.getKey()));
    }

    public void cancel(Order order, CancelEvent event) {
        Payment payment = paymentReader.getByOrderKey(order.getKey());
        validator.validate(order, payment);
        paymentGatewayProcessor.cancelPayment(new PaymentGatewayCancel(payment.getExternalPaymentKey(), "구매자 변심"));
        cancelTxHandler.completeCancelTx(order, payment, event);
    }
}
