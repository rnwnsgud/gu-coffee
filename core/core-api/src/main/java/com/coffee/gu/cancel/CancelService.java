package com.coffee.gu.cancel;

import com.coffee.gu.PaymentGatewayCancel;
import com.coffee.gu.Principal;
import com.coffee.gu.TransactionHistoryManager;
import com.coffee.gu.coupon.IssuedCouponManager;
import com.coffee.gu.order.Order;
import com.coffee.gu.order.OrderManager;
import com.coffee.gu.payment.Payment;
import com.coffee.gu.payment.PaymentGatewayProcessor;
import com.coffee.gu.payment.PaymentReader;
import com.coffee.gu.enums.TransactionType;
import org.springframework.stereotype.Service;

@Service
public class CancelService {

    private final CancelRollbacker cancelRollbacker;
    private final CancelRecorder cancelRecorder;
    private final CancelValidator validator;
    private final PaymentReader paymentReader;
    private final PaymentGatewayProcessor paymentGatewayProcessor;

    public CancelService(CancelRollbacker cancelRollbacker, CancelRecorder cancelRecorder, CancelValidator validator, PaymentReader paymentReader, PaymentGatewayProcessor paymentGatewayProcessor) {
        this.cancelRollbacker = cancelRollbacker;
        this.cancelRecorder = cancelRecorder;
        this.validator = validator;
        this.paymentReader = paymentReader;
        this.paymentGatewayProcessor = paymentGatewayProcessor;
    }

    public Long cancel(Order order, Principal principal) {
        Payment payment = paymentReader.getByOrderKey(order.getKey());
        // 검증(주문유효성, 주문취소가능여부)
        validator.validate(order, payment, principal);
        // pg 취소
        paymentGatewayProcessor.cancelPayment(new PaymentGatewayCancel(payment.getExternalPaymentKey(), "구매자 변심"));
        // 롤백
        cancelRollbacker.rollback(order, payment);
        // 기록
        return cancelRecorder.record(payment, order).getId();
    }
}
