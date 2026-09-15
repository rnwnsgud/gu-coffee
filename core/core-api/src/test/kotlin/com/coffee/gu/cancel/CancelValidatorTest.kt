package com.coffee.gu.cancel

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.order.Order
import com.coffee.gu.payment.Payment
import com.coffee.gu.stamp.StampRevertManager
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class CancelValidatorTest {

    @Mock
    private lateinit var stampRevertManager: StampRevertManager

    @InjectMocks
    private lateinit var cancelValidator: CancelValidator

    private lateinit var order: Order

    @BeforeEach
    fun setUp() {
        order = Order(
            key = "ORDER-VAL-1",
            name = "아메리카노",
            principal = Principal.user("U1"),
            storeId = 1L,
            totalPrice = BigDecimal.valueOf(3000),
            state = OrderState.PAID,
            lines = emptyList()
        )
    }

    @Test
    @DisplayName("결제 상태가 SUCCESS인 일반 취소 대상은 검증을 통과한다")
    fun validate_WhenPaymentIsSuccess_ShouldPass() {
        // given
        val payment = Payment(
            id = 1L,
            principal = Principal.user("U1"),
            orderKey = "ORDER-VAL-1",
            originalAmount = BigDecimal.valueOf(3000),
            issuedCouponId = null,
            couponDiscount = BigDecimal.ZERO,
            amount = BigDecimal.valueOf(3000),
            state = PaymentState.SUCCESS,
            externalPaymentKey = "PAY-KEY-SUCCESS",
            method = null,
            paidAt = null,
            approveCode = null,
            createdAt = LocalDateTime.now(),
            retryCount = 0
        )

        // when & then
        assertThatCode { cancelValidator.validate(order, payment) }
            .doesNotThrowAnyException()
        verify(stampRevertManager).validateRevertable(order)
    }

    @Test
    @DisplayName("결제 상태가 FAILED이지만 externalPaymentKey가 존재하는 보상 취소 대상은 검증을 통과한다")
    fun validate_WhenPaymentIsFailedWithExternalPaymentKey_ShouldPass() {
        // given
        val payment = Payment(
            id = 1L,
            principal = Principal.user("U1"),
            orderKey = "ORDER-VAL-1",
            originalAmount = BigDecimal.valueOf(3000),
            issuedCouponId = null,
            couponDiscount = BigDecimal.ZERO,
            amount = BigDecimal.valueOf(3000),
            state = PaymentState.FAILED,
            externalPaymentKey = "PAY-KEY-COMPENSATE",
            method = null,
            paidAt = null,
            approveCode = null,
            createdAt = LocalDateTime.now(),
            retryCount = 0
        )

        // when & then
        assertThatCode { cancelValidator.validate(order, payment) }
            .doesNotThrowAnyException()
        verify(stampRevertManager).validateRevertable(order)
    }

    @Test
    @DisplayName("결제 상태가 PENDING_PG이거나 READY인 미승인 건은 취소 검증에서 예외를 던진다")
    fun validate_WhenPaymentIsPendingOrReady_ShouldThrowException() {
        // given
        val pendingPayment = Payment(
            id = 1L,
            principal = Principal.user("U1"),
            orderKey = "ORDER-VAL-1",
            originalAmount = BigDecimal.valueOf(3000),
            issuedCouponId = null,
            couponDiscount = BigDecimal.ZERO,
            amount = BigDecimal.valueOf(3000),
            state = PaymentState.PENDING_PG,
            externalPaymentKey = null,
            method = null,
            paidAt = null,
            approveCode = null,
            createdAt = LocalDateTime.now(),
            retryCount = 0
        )

        // when & then
        assertThatThrownBy { cancelValidator.validate(order, pendingPayment) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.PAYMENT_INVALID_STATE)
    }

    @Test
    @DisplayName("결제 상태가 FAILED이지만 externalPaymentKey가 없는 건은 취소 검증에서 예외를 던진다")
    fun validate_WhenPaymentIsFailedWithoutExternalPaymentKey_ShouldThrowException() {
        // given
        val failedPaymentWithoutKey = Payment(
            id = 1L,
            principal = Principal.user("U1"),
            orderKey = "ORDER-VAL-1",
            originalAmount = BigDecimal.valueOf(3000),
            issuedCouponId = null,
            couponDiscount = BigDecimal.ZERO,
            amount = BigDecimal.valueOf(3000),
            state = PaymentState.FAILED,
            externalPaymentKey = null,
            method = null,
            paidAt = null,
            approveCode = null,
            createdAt = LocalDateTime.now(),
            retryCount = 0
        )

        // when & then
        assertThatThrownBy { cancelValidator.validate(order, failedPaymentWithoutKey) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.PAYMENT_INVALID_STATE)
    }
}
