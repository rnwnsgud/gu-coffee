package com.coffee.gu.support.pg

import com.coffee.gu.PGConfirmResult
import com.coffee.gu.PGPayment
import com.coffee.gu.PaymentGatewayStatus
import com.coffee.gu.Principal
import com.coffee.gu.cancel.CancelService
import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderReader
import com.coffee.gu.payment.Payment
import com.coffee.gu.payment.PaymentCompleter
import com.coffee.gu.payment.PaymentGatewayProcessor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PaymentRecoverSchedulerTest {

    @Mock
    private lateinit var paymentRecoveryProcessor: PaymentRecoveryProcessor

    @Mock
    private lateinit var orderReader: OrderReader

    @Mock
    private lateinit var paymentGatewayProcessor: PaymentGatewayProcessor

    @Mock
    private lateinit var paymentCompleter: PaymentCompleter

    @Mock
    private lateinit var cancelService: CancelService

    @InjectMocks
    private lateinit var scheduler: PaymentRecoverScheduler

    private lateinit var pendingPayment: Payment
    private lateinit var order: Order

    @BeforeEach
    fun setUp() {
        pendingPayment = Payment(
            id = 1L,
            principal = Principal.user("U1"),
            orderKey = "ORDER-REC-1",
            originalAmount = BigDecimal.valueOf(10000),
            issuedCouponId = null,
            couponDiscount = BigDecimal.ZERO,
            amount = BigDecimal.valueOf(10000),
            state = PaymentState.PENDING_PG,
            externalPaymentKey = "PAY-KEY-1",
            method = PaymentMethod.CARD,
            paidAt = null,
            approveCode = null,
            createdAt = LocalDateTime.now(),
            retryCount = 0
        )

        order = Order("ORDER-REC-1", "Coffee", Principal.user("U1"), 1L, BigDecimal.valueOf(10000), OrderState.CREATED, emptyList())
    }

    @Test
    @DisplayName("PG사 결제 상태가 DONE인 경우 추가 승인요청 없이 결제를 완료 처리한다")
    fun schedule_WhenPgPaymentStatusIsDone_ShouldCompleteWithoutApprovePayment() {
        // given
        given(paymentRecoveryProcessor.claimPendingPayments(PaymentRecoverScheduler.LIMIT)).willReturn(listOf(pendingPayment))
        given(orderReader.getByOrderKey("ORDER-REC-1")).willReturn(order)
        given(paymentGatewayProcessor.getPGPayment("ORDER-REC-1"))
            .willReturn(PGPayment("PAY-KEY-1", "ORDER-REC-1", BigDecimal.valueOf(10000), PaymentGatewayStatus.DONE))

        // when
        scheduler.schedule()

        // then
        verify(paymentGatewayProcessor, never()).approvePayment(any())
        verify(paymentCompleter).complete(eq(order), eq(1L), any())
    }

    @Test
    @DisplayName("PG사 결제 상태가 READY인 경우 유저 이탈로 간주하고 주문을 취소 처리한다")
    fun schedule_WhenPgPaymentStatusIsReady_ShouldCancelOrder() {
        // given
        given(paymentRecoveryProcessor.claimPendingPayments(PaymentRecoverScheduler.LIMIT)).willReturn(listOf(pendingPayment))
        given(orderReader.getByOrderKey("ORDER-REC-1")).willReturn(order)
        given(paymentGatewayProcessor.getPGPayment("ORDER-REC-1"))
            .willReturn(PGPayment("PAY-KEY-1", "ORDER-REC-1", BigDecimal.valueOf(10000), PaymentGatewayStatus.READY))

        // when
        scheduler.schedule()

        // then
        verify(paymentGatewayProcessor, never()).approvePayment(any())
        verify(cancelService).cancel(order)
    }

    @Test
    @DisplayName("PG사 결제 상태가 ABORTED인 경우 주문 및 결제를 취소 처리한다")
    fun schedule_WhenPgPaymentStatusIsAborted_ShouldCancelOrder() {
        // given
        given(paymentRecoveryProcessor.claimPendingPayments(PaymentRecoverScheduler.LIMIT)).willReturn(listOf(pendingPayment))
        given(orderReader.getByOrderKey("ORDER-REC-1")).willReturn(order)
        given(paymentGatewayProcessor.getPGPayment("ORDER-REC-1"))
            .willReturn(PGPayment("PAY-KEY-1", "ORDER-REC-1", BigDecimal.valueOf(10000), PaymentGatewayStatus.ABORTED))

        // when
        scheduler.schedule()

        // then
        verify(cancelService).cancel(order)
    }

    @Test
    @DisplayName("30분이 지난 PENDING_PG 결제건은 PG 조회 없이 즉시 만료 취소 처리한다")
    fun schedule_WhenPaymentIsExpired_ShouldCancelOrderWithoutPgCheck() {
        // given
        val expiredPayment = Payment(
            id = 2L,
            principal = Principal.user("U1"),
            orderKey = "ORDER-EXPIRED",
            originalAmount = BigDecimal.valueOf(10000),
            issuedCouponId = null,
            couponDiscount = BigDecimal.ZERO,
            amount = BigDecimal.valueOf(10000),
            state = PaymentState.PENDING_PG,
            externalPaymentKey = "PAY-KEY-2",
            method = PaymentMethod.CARD,
            paidAt = null,
            approveCode = null,
            createdAt = LocalDateTime.now().minusMinutes(40),
            retryCount = 0
        )

        given(paymentRecoveryProcessor.claimPendingPayments(PaymentRecoverScheduler.LIMIT)).willReturn(listOf(expiredPayment))
        given(orderReader.getByOrderKey("ORDER-EXPIRED")).willReturn(order)

        // when
        scheduler.schedule()

        // then
        verify(paymentGatewayProcessor, never()).getPGPayment(any())
        verify(cancelService).cancel(order)
    }

    @Test
    @DisplayName("PG 승인 처리 중 예외 발생 시 processor.handleRetry를 호출하여 재시도 백오프를 적용한다")
    fun schedule_WhenExceptionOccurs_ShouldCallHandleRetry() {
        // given
        given(paymentRecoveryProcessor.claimPendingPayments(PaymentRecoverScheduler.LIMIT)).willReturn(listOf(pendingPayment))
        given(orderReader.getByOrderKey("ORDER-REC-1")).willReturn(order)
        given(paymentGatewayProcessor.getPGPayment("ORDER-REC-1"))
            .willThrow(RuntimeException("PG Connection Failed"))

        // when
        scheduler.schedule()

        // then
        verify(paymentRecoveryProcessor).handleRetry(eq(pendingPayment), eq(5))
        verify(cancelService, never()).cancel(any())
    }

    @Test
    @DisplayName("만료된 결제건 취소 시 예외가 발생하면 processor.handleExpireFail을 호출한다")
    fun schedule_WhenExpiredCancelThrowsException_ShouldCallHandleExpireFail() {
        // given
        val expiredPayment = Payment(
            id = 2L,
            principal = Principal.user("U1"),
            orderKey = "ORDER-EXPIRED",
            originalAmount = BigDecimal.valueOf(10000),
            issuedCouponId = null,
            couponDiscount = BigDecimal.ZERO,
            amount = BigDecimal.valueOf(10000),
            state = PaymentState.PENDING_PG,
            externalPaymentKey = "PAY-KEY-2",
            method = PaymentMethod.CARD,
            paidAt = null,
            approveCode = null,
            createdAt = LocalDateTime.now().minusMinutes(40),
            retryCount = 0
        )

        given(paymentRecoveryProcessor.claimPendingPayments(PaymentRecoverScheduler.LIMIT)).willReturn(listOf(expiredPayment))
        given(orderReader.getByOrderKey("ORDER-EXPIRED")).willReturn(order)
        doThrow(RuntimeException("Cancel failed")).`when`(cancelService).cancel(order)

        // when
        scheduler.schedule()

        // then
        verify(paymentRecoveryProcessor).handleExpireFail(expiredPayment)
    }
}
