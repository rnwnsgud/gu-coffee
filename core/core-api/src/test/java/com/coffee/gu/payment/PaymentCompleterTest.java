package com.coffee.gu.payment;

import com.coffee.gu.CancelEvent;
import com.coffee.gu.PGConfirmResult;
import com.coffee.gu.Principal;
import com.coffee.gu.TransactionHistoryManager;
import com.coffee.gu.coupon.IssuedCouponManager;
import com.coffee.gu.enums.OrderState;
import com.coffee.gu.enums.PaymentMethod;
import com.coffee.gu.enums.PaymentState;
import com.coffee.gu.enums.TransactionType;
import com.coffee.gu.event.OutboxEventPublisher;
import com.coffee.gu.order.Order;
import com.coffee.gu.order.OrderManager;
import com.coffee.gu.stamp.StampHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentCompleterTest {

    @Mock
    private PaymentReader paymentReader;

    @Mock
    private PaymentManager paymentManager;

    @Mock
    private OrderManager orderManager;

    @Mock
    private IssuedCouponManager issuedCouponManager;

    @Mock
    private TransactionHistoryManager transactionHistoryManager;

    @Mock
    private StampHandler stampHandler;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private PaymentCompleter paymentCompleter;

    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        order = new Order(
                "ORDER-KEY-100",
                "아메리카노 1잔",
                Principal.user("U1"),
                1L,
                BigDecimal.valueOf(3000),
                OrderState.CREATED,
                List.of()
        );

        payment = new Payment(
                10L,
                Principal.user("U1"),
                "ORDER-KEY-100",
                BigDecimal.valueOf(3000),
                null,
                BigDecimal.ZERO,
                BigDecimal.valueOf(3000),
                PaymentState.PENDING_PG,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                0
        );
    }

    @Test
    @DisplayName("compensateApprovalFailure: 결제 실패 전이, 외부 키 보존, 실패 이력 저장, Outbox CancelEvent 저장이 단일 보상 트랜잭션 내에서 원자적으로 처리된다")
    void compensateApprovalFailure_ShouldAtomicallyUpdatePaymentRecordHistoryAndPublishOutboxEvent() {
        // given
        long paymentId = 10L;
        String externalPaymentKey = "PG-PAY-KEY-999";
        String failureReason = "쿠폰 만료로 인한 결제 완료 처리 실패";

        given(paymentReader.getByIdWithLock(paymentId)).willReturn(payment);

        // when
        PaymentApprovalResult result = paymentCompleter.compensateApprovalFailure(
                order,
                paymentId,
                externalPaymentKey,
                failureReason
        );

        // then
        // 1. Payment 엔티티 상태가 FAILED로 변경되고 externalPaymentKey가 정상 기록되어야 함
        assertThat(payment.getState()).isEqualTo(PaymentState.FAILED);
        assertThat(payment.getExternalPaymentKey()).isEqualTo(externalPaymentKey);
        verify(paymentManager).save(payment);

        // 2. TransactionHistoryManager에 PAYMENT_FAIL 이력이 기록되어야 함
        verify(transactionHistoryManager).record(
                eq(TransactionType.PAYMENT_FAIL),
                eq(order),
                eq(payment),
                eq(failureReason),
                any(OffsetDateTime.class)
        );

        // 3. OutboxEventPublisher에 CancelEvent가 단일 원자적 흐름으로 발행 및 저장되어야 함
        ArgumentCaptor<CancelEvent> eventCaptor = ArgumentCaptor.forClass(CancelEvent.class);
        verify(outboxEventPublisher).publishOutboxEvent(eventCaptor.capture());
        CancelEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getOrderKey()).isEqualTo("ORDER-KEY-100");

        // 4. 반환된 결과가 FAILED 상태여야 함
        assertThat(result.getPaymentState()).isEqualTo(PaymentState.FAILED);
        assertThat(result.getPaymentKey()).isEqualTo(externalPaymentKey);
    }

    @Test
    @DisplayName("complete: 정상 승인 시 결제 성공 상태 전이, 주문 결제 처리, 이력 기록이 정상 수행된다")
    void complete_WhenSuccessful_ShouldTransitionToPaid() {
        // given
        long paymentId = 10L;
        PGConfirmResult confirmed = PGConfirmResult.success(
                "ORDER-KEY-100",
                "PG-PAY-KEY-999",
                PaymentMethod.CARD,
                "APPR-001",
                OffsetDateTime.now()
        );

        given(paymentReader.getByIdWithLock(paymentId)).willReturn(payment);

        // when
        PaymentApprovalResult result = paymentCompleter.complete(order, paymentId, confirmed);

        // then
        assertThat(result.getPaymentState()).isEqualTo(PaymentState.SUCCESS);
        verify(paymentManager).pay(payment, confirmed);
        verify(orderManager).pay(order);
        verify(issuedCouponManager).use(payment);
        verify(transactionHistoryManager).record(eq(TransactionType.PAYMENT), eq(order), eq(payment), any(), any());
    }
}
