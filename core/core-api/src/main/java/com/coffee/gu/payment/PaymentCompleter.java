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

    /// PG WEBHOOK 이 없거나 유실될 시 멱등성 고려
    /// 만약 해당 로직이 중복처리가 된다면...
    /// 1. 추후 order 후 배송 이벤트를 전송할때 중복으로 된다던가
    /// 2. 동일한 쿠폰에 대해 이미 사용된 쿠폰 예외가 발생되어서 결제는 성공했는데 트랜잭션이 롤백될 수 있음
    /// 3. 중요한 정산 데이터의 중복
    @Transactional
    public PaymentApprovalResult complete(Order order, Long paymentId, PGConfirmResult confirmedPayment) {
        try {
            Payment payment = paymentReader.getByIdWithLock(paymentId);
            if (payment.isPaid()) return PaymentApprovalResult.alReadyApproved(order.getKey(), payment.getExternalPaymentKey(), payment.getPaidAt());
            if (!confirmedPayment.isConfirmed()) {
                payment.fail();
                paymentManager.prepare(payment); // fail 상태 저장을 위해 prepare 재활용하거나 별도 메서드 필요. 여기서는 일단 흐름 유지
                return PaymentApprovalResult.failed(order.getKey(), payment.getExternalPaymentKey(), payment.getPaidAt());
            }
            paymentManager.pay(payment, confirmedPayment);
            orderManager.pay(order);
            issuedCouponManager.use(payment);
            transactionHistoryManager.record(TransactionType.PAYMENT, order, payment, "Payment processed", payment.getPaidAt());
            return PaymentApprovalResult.approved(order.getKey(), payment.getExternalPaymentKey(), payment.getPaidAt());
        } catch (Exception e) {
            /// !!! 결제승인 됐는데 롤백 된다면? -> 보상트랜잭션(망취소) 혹은 기타 정합성 유지 로직 필요
            throw new CoreException(ErrorType.PAYMENT_FAIL, null);
        }
    }

    @Transactional
    public void failProcess(Order order, Payment payment, String code, String message) {
        payment.fail();
        paymentManager.prepare(payment);
        stampHandler.revert(order);
        transactionHistoryManager.record(TransactionType.PAYMENT_FAIL, order, payment, String.format("%s %s", code, message), OffsetDateTime.now());
    }
}
