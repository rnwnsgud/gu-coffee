package com.coffee.gu.payment

import com.coffee.gu.PGConfirmResult
import com.coffee.gu.PGPayment
import com.coffee.gu.PaymentGatewayStatus
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.order.Order
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
class PaymentServiceTimeoutTest {

    @Mock
    private lateinit var paymentGatewayProcessor: PaymentGatewayProcessor

    @Mock
    private lateinit var paymentPreparer: PaymentPreparer

    @Mock
    private lateinit var paymentCompleter: PaymentCompleter

    @Mock
    private lateinit var paymentManager: PaymentManager

    @Mock
    private lateinit var paymentReader: PaymentReader

    @InjectMocks
    private lateinit var paymentService: PaymentService

    private lateinit var order: Order
    private lateinit var payment: Payment
    private lateinit var initialPgPayment: PGPayment

    private fun createPreparedPayment(): Payment = Payment(
        id = 1L,
        principal = Principal.user("U1"),
        orderKey = "ORDER-TIMEOUT-1",
        originalAmount = BigDecimal.valueOf(3000),
        issuedCouponId = null,
        couponDiscount = null,
        amount = BigDecimal.valueOf(3000),
        state = PaymentState.PENDING_PG,
        externalPaymentKey = "PAY-KEY-1",
        method = PaymentMethod.CARD,
        paidAt = null,
        approveCode = null,
        createdAt = LocalDateTime.now(),
        retryCount = 0
    )

    @BeforeEach
    fun setUp() {
        order = Order(
            key = "ORDER-TIMEOUT-1",
            name = "아메리카노",
            principal = Principal.user("U1"),
            storeId = 1L,
            totalPrice = BigDecimal.valueOf(3000),
            state = OrderState.CREATED,
            lines = emptyList()
        )

        payment = Payment(
            id = 1L,
            principal = Principal.user("U1"),
            orderKey = "ORDER-TIMEOUT-1",
            originalAmount = BigDecimal.valueOf(3000),
            issuedCouponId = null,
            couponDiscount = null,
            amount = BigDecimal.valueOf(3000),
            state = PaymentState.READY,
            externalPaymentKey = null,
            method = null,
            paidAt = null,
            approveCode = null,
            createdAt = LocalDateTime.now(),
            retryCount = 0
        )

        initialPgPayment = PGPayment(
            paymentKey = "PAY-KEY-1",
            orderKey = "ORDER-TIMEOUT-1",
            amount = BigDecimal.valueOf(3000),
            status = PaymentGatewayStatus.READY
        )
    }

    @Test
    @DisplayName("시나리오 1: PG 승인 요청 중 Read Timeout 발생 시, 즉시 조회로 DONE 확인되면 0초 만에 인라인 정상 승인 복구한다")
    fun approvePayment_WhenApproveThrowsTimeout_AndStatusIsDone_ShouldRecoverAndComplete() {
        // given
        val preparedPayment = createPreparedPayment()

        given(paymentReader.getByOrderKey("ORDER-TIMEOUT-1")).willReturn(payment)
        given(paymentGatewayProcessor.getPGPayment("ORDER-TIMEOUT-1"))
            .willReturn(initialPgPayment)
            .willReturn(PGPayment("PAY-KEY-1", "ORDER-TIMEOUT-1", BigDecimal.valueOf(3000), PaymentGatewayStatus.DONE))
        given(paymentPreparer.prepare(order, initialPgPayment)).willReturn(preparedPayment)
        given(paymentGatewayProcessor.approvePayment(any()))
            .willThrow(RuntimeException("SocketTimeoutException: Read timed out"))

        val expectedResult = PaymentApprovalResult.approved("ORDER-TIMEOUT-1", "PAY-KEY-1", OffsetDateTime.now())
        given(paymentCompleter.complete(eq(order), eq(1L), any()))
            .willReturn(expectedResult)

        // when
        val result = paymentService.approvePayment(order)

        // then
        assertThat(result.paymentState).isEqualTo(PaymentState.SUCCESS)
        assertThat(result.orderState).isEqualTo(OrderState.PAID)
        verify(paymentCompleter).complete(eq(order), eq(1L), any())
        verify(paymentCompleter, never()).failProcess(any(), any(), any(), any())
    }

    @Test
    @DisplayName("시나리오 2: PG 승인 요청 중 타임아웃 발생 시, 즉시 조회로 미승인(READY) 확인되면 즉시 실패 확정 처리한다")
    fun approvePayment_WhenApproveThrowsTimeout_AndStatusIsNotDone_ShouldFailProcess() {
        // given
        val preparedPayment = createPreparedPayment()

        given(paymentReader.getByOrderKey("ORDER-TIMEOUT-1")).willReturn(payment)
        given(paymentGatewayProcessor.getPGPayment("ORDER-TIMEOUT-1"))
            .willReturn(initialPgPayment)
            .willReturn(PGPayment("PAY-KEY-1", "ORDER-TIMEOUT-1", BigDecimal.valueOf(3000), PaymentGatewayStatus.READY))
        given(paymentPreparer.prepare(order, initialPgPayment)).willReturn(preparedPayment)
        given(paymentGatewayProcessor.approvePayment(any()))
            .willThrow(RuntimeException("SocketTimeoutException: Read timed out"))

        // when
        val result = paymentService.approvePayment(order)

        // then
        assertThat(result.paymentState).isEqualTo(PaymentState.FAILED)
        verify(paymentCompleter).failProcess(eq(order), eq(preparedPayment), eq("PG_APPROVE_TIMEOUT"), any())
        verify(paymentCompleter, never()).complete(any(), any(), any())
    }

    @Test
    @DisplayName("시나리오 3: 승인 및 상태 조회 모두 네트워크 단절 시, 즉시 자동 망취소를 시도하고 취소 실패 시 Outbox로 비동기 보상 이벤트를 발행한다")
    fun approvePayment_WhenApproveAndInquiryBothTimeout_ShouldCancelPaymentAndPublishOutbox() {
        // given
        val preparedPayment = createPreparedPayment()

        given(paymentReader.getByOrderKey("ORDER-TIMEOUT-1")).willReturn(payment)
        given(paymentGatewayProcessor.getPGPayment("ORDER-TIMEOUT-1"))
            .willReturn(initialPgPayment)
            .willThrow(RuntimeException("Inquiry Connection Timeout"))
        given(paymentPreparer.prepare(order, initialPgPayment)).willReturn(preparedPayment)
        given(paymentGatewayProcessor.approvePayment(any()))
            .willThrow(RuntimeException("SocketTimeoutException: Read timed out"))
        given(paymentGatewayProcessor.cancelPayment(any()))
            .willThrow(RuntimeException("Cancel network error"))
        given(paymentCompleter.compensateApprovalFailure(eq(order), eq(1L), eq("PAY-KEY-1"), any()))
            .willReturn(PaymentApprovalResult.failed("ORDER-TIMEOUT-1", "PAY-KEY-1", OffsetDateTime.now()))

        // when
        val result = paymentService.approvePayment(order)

        // then
        assertThat(result.paymentState).isEqualTo(PaymentState.FAILED)
        verify(paymentGatewayProcessor).cancelPayment(any())
        verify(paymentCompleter).compensateApprovalFailure(eq(order), eq(1L), eq("PAY-KEY-1"), any())
        verify(paymentCompleter, never()).failProcess(any(), any(), any(), any())
    }

    @Test
    @DisplayName("시나리오 4: PG 승인은 성공했으나 complete 내부 로직 예외 발생 시, 원자적 보상 트랜잭션(compensateApprovalFailure)을 호출한다")
    fun approvePayment_WhenCompleteThrowsException_ShouldCompensateApprovalFailure() {
        // given
        val preparedPayment = createPreparedPayment()

        given(paymentReader.getByOrderKey("ORDER-TIMEOUT-1")).willReturn(payment)
        given(paymentGatewayProcessor.getPGPayment("ORDER-TIMEOUT-1")).willReturn(initialPgPayment)
        given(paymentPreparer.prepare(order, initialPgPayment)).willReturn(preparedPayment)
        given(paymentGatewayProcessor.approvePayment(any()))
            .willReturn(PGConfirmResult.success("ORDER-TIMEOUT-1", "PAY-KEY-1", PaymentMethod.CARD, "APPR-1", OffsetDateTime.now()))
        given(paymentCompleter.complete(eq(order), eq(1L), any()))
            .willThrow(RuntimeException("Coupon expired exception"))
        given(paymentCompleter.compensateApprovalFailure(eq(order), eq(1L), eq("PAY-KEY-1"), any()))
            .willReturn(PaymentApprovalResult.failed("ORDER-TIMEOUT-1", "PAY-KEY-1", OffsetDateTime.now()))

        // when
        val result = paymentService.approvePayment(order)

        // then
        assertThat(result.paymentState).isEqualTo(PaymentState.FAILED)
        verify(paymentCompleter).compensateApprovalFailure(eq(order), eq(1L), eq("PAY-KEY-1"), any())
    }
}
