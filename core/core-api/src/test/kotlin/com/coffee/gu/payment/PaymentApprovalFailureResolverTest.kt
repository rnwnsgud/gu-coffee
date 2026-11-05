package com.coffee.gu.payment

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.PGPayment
import com.coffee.gu.PaymentGatewayCancel
import com.coffee.gu.PaymentGatewayStatus
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.menu.LimitedMenuStockManager
import com.coffee.gu.order.Order
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.BDDMockito.willThrow
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import java.math.BigDecimal
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PaymentApprovalFailureResolverTest {

    @Mock
    private lateinit var paymentGatewayProcessor: PaymentGatewayProcessor

    @Mock
    private lateinit var paymentCompleter: PaymentCompleter

    @Mock
    private lateinit var limitedMenuStockManager: LimitedMenuStockManager

    @InjectMocks
    private lateinit var resolver: PaymentApprovalFailureResolver

    private lateinit var order: Order
    private lateinit var payment: Payment
    private lateinit var pgPayment: PGPayment

    @BeforeEach
    fun setUp() {
        order = Order(
            key = "ORD-TEST-1",
            name = "테스트 주문",
            principal = Principal.user("U1"),
            storeId = 1L,
            totalPrice = BigDecimal.valueOf(10000),
            state = OrderState.CREATED,
            lines = emptyList(),
        )

        payment = Payment(
            id = 1L,
            principal = Principal.user("U1"),
            orderKey = "ORD-TEST-1",
            originalAmount = BigDecimal.valueOf(10000),
            issuedCouponId = null,
            couponDiscount = null,
            amount = BigDecimal.valueOf(10000),
            state = PaymentState.PENDING_PG,
            externalPaymentKey = "PAY-KEY-1",
            method = PaymentMethod.CARD,
            paidAt = null,
            approveCode = null,
            createdAt = LocalDateTime.now(),
            retryCount = 0,
        )

        pgPayment = PGPayment(
            paymentKey = "PAY-KEY-1",
            orderKey = "ORD-TEST-1",
            amount = BigDecimal.valueOf(10000),
            status = PaymentGatewayStatus.READY,
        )
    }

    @Test
    @DisplayName("선점 만료 시 아직 결제 전(status!=DONE)이면 failProcess 호출 후 LIMITED_MENU_RESERVATION_EXPIRED 예외를 던진다")
    fun handleExpiredReservation_notDone_failsProcess() {
        assertThatThrownBy {
            resolver.handleExpiredReservation(order, payment, pgPayment)
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.LIMITED_MENU_RESERVATION_EXPIRED)

        then(paymentCompleter).should().failProcess(
            eq(order),
            eq(payment),
            eq(ErrorType.LIMITED_MENU_RESERVATION_EXPIRED.code.name),
            eq(ErrorType.LIMITED_MENU_RESERVATION_EXPIRED.message),
        )
        then(paymentGatewayProcessor).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("선점 만료 시 이미 결제 완료(status==DONE) 상태라면 autoCancelOversoldPayment를 호출하여 PG 취소를 시도한다")
    fun handleExpiredReservation_done_autoCancels() {
        val donePgPayment = pgPayment.copy(status = PaymentGatewayStatus.DONE)

        assertThatThrownBy {
            resolver.handleExpiredReservation(order, payment, donePgPayment)
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.LIMITED_MENU_OVERSOLD_AUTO_CANCELED)

        then(paymentGatewayProcessor).should().cancelPayment(
            PaymentGatewayCancel("PAY-KEY-1", ErrorType.LIMITED_MENU_OVERSOLD_AUTO_CANCELED.message)
        )
        then(paymentCompleter).should().failProcess(
            eq(order),
            eq(payment),
            eq(ErrorType.LIMITED_MENU_OVERSOLD_AUTO_CANCELED.code.name),
            eq(ErrorType.LIMITED_MENU_OVERSOLD_AUTO_CANCELED.message),
        )
    }

    @Test
    @DisplayName("초과 판매 취소 중 PG 취소가 실패하면 아웃박스 보상 트랜잭션(compensateApprovalFailure)을 적재한다")
    fun autoCancelOversoldPayment_cancelFailed_compensates() {
        willThrow(RuntimeException("PG 취소 네트워크 타임아웃"))
            .given(paymentGatewayProcessor).cancelPayment(any())

        assertThatThrownBy {
            resolver.autoCancelOversoldPayment(order, payment, "PAY-KEY-1")
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.LIMITED_MENU_OVERSOLD_AUTO_CANCELED)

        then(paymentCompleter).should().compensateApprovalFailure(
            eq(order),
            eq(payment.id),
            eq("PAY-KEY-1"),
            eq(ErrorType.LIMITED_MENU_OVERSOLD_AUTO_CANCELED.message),
        )
    }

    @Test
    @DisplayName("승인 실패 복구 시 현재 PG 상태가 DONE이면 정상 완료(complete) 처리한다")
    fun resolveApprovalFailure_statusDone_completes() {
        val donePgPayment = PGPayment("PAY-KEY-1", "ORD-TEST-1", BigDecimal.valueOf(10000), PaymentGatewayStatus.DONE)
        given(paymentGatewayProcessor.getPGPayment(order.key)).willReturn(donePgPayment)

        val successResult = PaymentApprovalResult.approved(order.key, "PAY-KEY-1", java.time.OffsetDateTime.now())
        given(paymentCompleter.complete(eq(order), eq(payment.id), any())).willReturn(successResult)

        val result = resolver.resolveApprovalFailure(order, payment, pgPayment, mapOf(100L to 1))

        assertThat(result.paymentState).isEqualTo(PaymentState.SUCCESS)
        then(paymentCompleter).should().complete(eq(order), eq(payment.id), any())
    }

    @Test
    @DisplayName("승인 실패 복구 시 현재 PG 상태가 미승인이면 재고를 복구하고 failProcess 처리한다")
    fun resolveApprovalFailure_notDone_releasesStockAndFails() {
        val readyPgPayment = PGPayment("PAY-KEY-1", "ORD-TEST-1", BigDecimal.valueOf(10000), PaymentGatewayStatus.READY)
        given(paymentGatewayProcessor.getPGPayment(order.key)).willReturn(readyPgPayment)

        val result = resolver.resolveApprovalFailure(order, payment, pgPayment, mapOf(100L to 1))

        assertThat(result.paymentState).isEqualTo(PaymentState.FAILED)
        then(limitedMenuStockManager).should().releaseIfLimited(
            orderKey = eq(order.key),
            principalKey = eq(order.principal.key),
            menuQuantities = eq(mapOf(100L to 1)),
            restoreDbSoldQuantity = eq(false),
            nowEpochMillis = any(),
        )
        then(paymentCompleter).should().failProcess(order, payment, "PG_APPROVE_TIMEOUT", "승인 타임아웃 후 미승인 확인")
    }
}
