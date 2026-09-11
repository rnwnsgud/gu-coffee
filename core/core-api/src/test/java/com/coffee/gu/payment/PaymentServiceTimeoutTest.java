package com.coffee.gu.payment;

import com.coffee.gu.CancelEvent;
import com.coffee.gu.PGConfirmResult;
import com.coffee.gu.PGPayment;
import com.coffee.gu.PaymentGatewayCancel;
import com.coffee.gu.PaymentGatewayConfirm;
import com.coffee.gu.PaymentGatewayStatus;
import com.coffee.gu.Principal;
import com.coffee.gu.enums.OrderState;
import com.coffee.gu.enums.PaymentMethod;
import com.coffee.gu.enums.PaymentState;
import com.coffee.gu.event.OutboxEventPublisher;
import com.coffee.gu.order.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTimeoutTest {

    @Mock
    private PaymentGatewayProcessor paymentGatewayProcessor;

    @Mock
    private PaymentPreparer paymentPreparer;

    @Mock
    private PaymentCompleter paymentCompleter;

    @Mock
    private PaymentManager paymentManager;

    @Mock
    private PaymentReader paymentReader;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private Order order;
    private Payment payment;
    private PGPayment initialPgPayment;

    @BeforeEach
    void setUp() {
        order = new Order(
                "ORDER-TIMEOUT-1",
                "아메리카노",
                Principal.user("U1"),
                1L,
                BigDecimal.valueOf(3000),
                OrderState.CREATED,
                List.of()
        );

        payment = new Payment(
                1L,
                Principal.user("U1"),
                "ORDER-TIMEOUT-1",
                BigDecimal.valueOf(3000),
                null,
                BigDecimal.ZERO,
                BigDecimal.valueOf(3000),
                PaymentState.READY,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                0
        );

        initialPgPayment = new PGPayment(
                "PAY-KEY-1",
                "ORDER-TIMEOUT-1",
                BigDecimal.valueOf(3000),
                PaymentGatewayStatus.READY
        );
    }

    @Test
    @DisplayName("시나리오 1: PG 승인 요청 중 Read Timeout 발생 시, 즉시 조회로 DONE 확인되면 0초 만에 인라인 정상 승인 복구한다")
    void approvePayment_WhenApproveThrowsTimeout_AndStatusIsDone_ShouldRecoverAndComplete() {
        // given
        Payment preparedPayment = new Payment(
                1L,
                Principal.user("U1"),
                "ORDER-TIMEOUT-1",
                BigDecimal.valueOf(3000),
                null,
                BigDecimal.ZERO,
                BigDecimal.valueOf(3000),
                PaymentState.PENDING_PG,
                "PAY-KEY-1",
                PaymentMethod.CARD,
                null,
                null,
                LocalDateTime.now(),
                0
        );

        given(paymentReader.getByOrderKey("ORDER-TIMEOUT-1")).willReturn(payment);
        given(paymentGatewayProcessor.getPGPayment("ORDER-TIMEOUT-1"))
                .willReturn(initialPgPayment) // 1회차: 준비 단계
                .willReturn(new PGPayment("PAY-KEY-1", "ORDER-TIMEOUT-1", BigDecimal.valueOf(3000), PaymentGatewayStatus.DONE)); // 2회차: 타임아웃 즉시 조회 시 DONE 확인
        given(paymentPreparer.prepare(order, initialPgPayment)).willReturn(preparedPayment);
        given(paymentGatewayProcessor.approvePayment(any(PaymentGatewayConfirm.class)))
                .willThrow(new RuntimeException("SocketTimeoutException: Read timed out"));

        PaymentApprovalResult expectedResult = PaymentApprovalResult.approved("ORDER-TIMEOUT-1", "PAY-KEY-1", OffsetDateTime.now());
        given(paymentCompleter.complete(eq(order), eq(1L), any(PGConfirmResult.class)))
                .willReturn(expectedResult);

        // when
        PaymentApprovalResult result = paymentService.approvePayment(order);

        // then
        assertThat(result.getPaymentState()).isEqualTo(PaymentState.SUCCESS);
        assertThat(result.getOrderState()).isEqualTo(OrderState.PAID);
        verify(paymentCompleter).complete(eq(order), eq(1L), any(PGConfirmResult.class));
        verify(paymentCompleter, never()).failProcess(any(), any(), any(), any());
        verify(outboxEventPublisher, never()).publishOutboxEvent(any());
    }

    @Test
    @DisplayName("시나리오 2: PG 승인 요청 중 타임아웃 발생 시, 즉시 조회로 미승인(READY) 확인되면 즉시 실패 확정 처리한다")
    void approvePayment_WhenApproveThrowsTimeout_AndStatusIsNotDone_ShouldFailProcess() {
        // given
        Payment preparedPayment = new Payment(
                1L,
                Principal.user("U1"),
                "ORDER-TIMEOUT-1",
                BigDecimal.valueOf(3000),
                null,
                BigDecimal.ZERO,
                BigDecimal.valueOf(3000),
                PaymentState.PENDING_PG,
                "PAY-KEY-1",
                PaymentMethod.CARD,
                null,
                null,
                LocalDateTime.now(),
                0
        );

        given(paymentReader.getByOrderKey("ORDER-TIMEOUT-1")).willReturn(payment);
        given(paymentGatewayProcessor.getPGPayment("ORDER-TIMEOUT-1"))
                .willReturn(initialPgPayment) // 1회차: 준비 단계
                .willReturn(new PGPayment("PAY-KEY-1", "ORDER-TIMEOUT-1", BigDecimal.valueOf(3000), PaymentGatewayStatus.READY)); // 2회차: 미승인 확인
        given(paymentPreparer.prepare(order, initialPgPayment)).willReturn(preparedPayment);
        given(paymentGatewayProcessor.approvePayment(any(PaymentGatewayConfirm.class)))
                .willThrow(new RuntimeException("SocketTimeoutException: Read timed out"));

        // when
        PaymentApprovalResult result = paymentService.approvePayment(order);

        // then
        assertThat(result.getPaymentState()).isEqualTo(PaymentState.FAILED);
        verify(paymentCompleter).failProcess(eq(order), eq(preparedPayment), eq("PG_APPROVE_TIMEOUT"), any());
        verify(paymentCompleter, never()).complete(any(), anyLong(), any());
    }

    @Test
    @DisplayName("시나리오 3: 승인 및 상태 조회 모두 네트워크 단절 시, 즉시 자동 망취소를 시도하고 취소 실패 시 Outbox로 비동기 보상 이벤트를 발행한다")
    void approvePayment_WhenApproveAndInquiryBothTimeout_ShouldCancelPaymentAndPublishOutbox() {
        // given
        Payment preparedPayment = new Payment(
                1L,
                Principal.user("U1"),
                "ORDER-TIMEOUT-1",
                BigDecimal.valueOf(3000),
                null,
                BigDecimal.ZERO,
                BigDecimal.valueOf(3000),
                PaymentState.PENDING_PG,
                "PAY-KEY-1",
                PaymentMethod.CARD,
                null,
                null,
                LocalDateTime.now(),
                0
        );

        given(paymentReader.getByOrderKey("ORDER-TIMEOUT-1")).willReturn(payment);
        given(paymentGatewayProcessor.getPGPayment("ORDER-TIMEOUT-1"))
                .willReturn(initialPgPayment) // 1회차: 준비 단계
                .willThrow(new RuntimeException("Inquiry Connection Timeout")); // 2회차: 상태 조회마저 타임아웃
        given(paymentPreparer.prepare(order, initialPgPayment)).willReturn(preparedPayment);
        given(paymentGatewayProcessor.approvePayment(any(PaymentGatewayConfirm.class)))
                .willThrow(new RuntimeException("SocketTimeoutException: Read timed out"));
        given(paymentGatewayProcessor.cancelPayment(any(PaymentGatewayCancel.class)))
                .willThrow(new RuntimeException("Cancel network error")); // 망취소 API 호출마저 실패

        // when
        PaymentApprovalResult result = paymentService.approvePayment(order);

        // then
        assertThat(result.getPaymentState()).isEqualTo(PaymentState.FAILED);
        // 망취소 시도 확인
        verify(paymentGatewayProcessor).cancelPayment(any(PaymentGatewayCancel.class));
        // 망취소 실패 시 Outbox 보상 이벤트 발행 확인
        verify(outboxEventPublisher).publishOutboxEvent(any(CancelEvent.class));
        // 결제 실패 처리 확인 (500 에러 없이 정돈된 FAILED 응답 반환)
        verify(paymentCompleter).failProcess(eq(order), eq(preparedPayment), eq("NETWORK_TIMEOUT"), any());
    }
}
