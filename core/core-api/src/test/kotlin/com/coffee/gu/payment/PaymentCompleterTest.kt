package com.coffee.gu.payment

import com.coffee.gu.CancelEvent
import com.coffee.gu.PGConfirmResult
import com.coffee.gu.Principal
import com.coffee.gu.TransactionHistoryManager
import com.coffee.gu.coupon.IssuedCouponManager
import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.enums.TransactionType
import com.coffee.gu.event.OutboxEventPublisher
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderManager
import com.coffee.gu.stamp.StampHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
class PaymentCompleterTest {

    @Mock
    private lateinit var paymentReader: PaymentReader

    @Mock
    private lateinit var paymentManager: PaymentManager

    @Mock
    private lateinit var orderManager: OrderManager

    @Mock
    private lateinit var issuedCouponManager: IssuedCouponManager

    @Mock
    private lateinit var transactionHistoryManager: TransactionHistoryManager

    @Mock
    private lateinit var stampHandler: StampHandler

    @Mock
    private lateinit var outboxEventPublisher: OutboxEventPublisher

    @InjectMocks
    private lateinit var paymentCompleter: PaymentCompleter

    private lateinit var order: Order
    private lateinit var payment: Payment

    @BeforeEach
    fun setUp() {
        order = Order(
            key = "ORDER-KEY-100",
            name = "아메리카노 1잔",
            principal = Principal.user("U1"),
            storeId = 1L,
            totalPrice = BigDecimal.valueOf(3000),
            state = OrderState.CREATED,
            lines = emptyList()
        )

        payment = Payment(
            id = 10L,
            principal = Principal.user("U1"),
            orderKey = "ORDER-KEY-100",
            originalAmount = BigDecimal.valueOf(3000),
            issuedCouponId = null,
            couponDiscount = null,
            amount = BigDecimal.valueOf(3000),
            state = PaymentState.PENDING_PG,
            externalPaymentKey = null,
            method = null,
            paidAt = null,
            approveCode = null,
            createdAt = LocalDateTime.now(),
            retryCount = 0
        )
    }

    @Test
    @DisplayName("compensateApprovalFailure: 결제 실패 전이, 외부 키 보존, 실패 이력 저장, Outbox CancelEvent 저장이 단일 보상 트랜잭션 내에서 원자적으로 처리된다")
    fun compensateApprovalFailure_ShouldAtomicallyUpdatePaymentRecordHistoryAndPublishOutboxEvent() {
        // given
        val paymentId = 10L
        val externalPaymentKey = "PG-PAY-KEY-999"
        val failureReason = "쿠폰 만료로 인한 결제 완료 처리 실패"

        given(paymentReader.getByIdWithLock(paymentId)).willReturn(payment)

        // when
        val result = paymentCompleter.compensateApprovalFailure(
            order,
            paymentId,
            externalPaymentKey,
            failureReason
        )

        // then
        // 1. Payment 엔티티 상태가 FAILED로 변경되고 externalPaymentKey가 정상 기록되어야 함
        assertThat(payment.state).isEqualTo(PaymentState.FAILED)
        assertThat(payment.externalPaymentKey).isEqualTo(externalPaymentKey)
        verify(paymentManager).save(payment)

        // 2. TransactionHistoryManager에 PAYMENT_FAIL 이력이 기록되어야 함
        verify(transactionHistoryManager).record(
            eq(TransactionType.PAYMENT_FAIL),
            eq(order),
            eq(payment),
            eq(failureReason),
            any()
        )

        // 3. OutboxEventPublisher에 CancelEvent가 단일 원자적 흐름으로 발행 및 저장되어야 함
        val eventCaptor = argumentCaptor<CancelEvent>()
        verify(outboxEventPublisher).publishOutboxEvent(eventCaptor.capture())
        val publishedEvent = eventCaptor.firstValue
        assertThat(publishedEvent.orderKey).isEqualTo("ORDER-KEY-100")

        // 4. 반환된 결과가 FAILED 상태여야 함
        assertThat(result.paymentState).isEqualTo(PaymentState.FAILED)
        assertThat(result.paymentKey).isEqualTo(externalPaymentKey)
    }

    @Test
    @DisplayName("complete: 정상 승인 시 결제 성공 상태 전이, 주문 결제 처리, 이력 기록이 정상 수행된다")
    fun complete_WhenSuccessful_ShouldTransitionToPaid() {
        // given
        val paymentId = 10L
        val confirmed = PGConfirmResult.success(
            "ORDER-KEY-100",
            "PG-PAY-KEY-999",
            PaymentMethod.CARD,
            "APPR-001",
            OffsetDateTime.now()
        )

        given(paymentReader.getByIdWithLock(paymentId)).willReturn(payment)

        // when
        val result = paymentCompleter.complete(order, paymentId, confirmed)

        // then
        assertThat(result.paymentState).isEqualTo(PaymentState.SUCCESS)
        verify(paymentManager).pay(payment, confirmed)
        verify(orderManager).pay(order)
        verify(issuedCouponManager).use(payment)
        verify(transactionHistoryManager).record(eq(TransactionType.PAYMENT), eq(order), eq(payment), anyOrNull(), anyOrNull())
    }
}
