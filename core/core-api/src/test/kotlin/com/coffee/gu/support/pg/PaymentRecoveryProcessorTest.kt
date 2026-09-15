package com.coffee.gu.support.pg

import com.coffee.gu.Principal
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.payment.Payment
import com.coffee.gu.payment.PaymentManager
import com.coffee.gu.payment.PaymentReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class PaymentRecoveryProcessorTest {

    @Mock
    private lateinit var paymentReader: PaymentReader

    @Mock
    private lateinit var paymentManager: PaymentManager

    @InjectMocks
    private lateinit var processor: PaymentRecoveryProcessor

    private lateinit var payment: Payment

    @BeforeEach
    fun setUp() {
        payment = Payment(
            id = 1L,
            principal = Principal.user("U1"),
            orderKey = "ORDER-1",
            originalAmount = BigDecimal.valueOf(10000),
            issuedCouponId = null,
            couponDiscount = BigDecimal.ZERO,
            amount = BigDecimal.valueOf(10000),
            state = PaymentState.PENDING_PG,
            externalPaymentKey = "PAY-KEY-1",
            method = PaymentMethod.CARD,
            paidAt = null,
            approveCode = null,
            createdAt = LocalDateTime.now().minusMinutes(10),
            retryCount = 0
        )
    }

    @Test
    @DisplayName("claimPendingPayments 호출 시 SKIP LOCKED 대상을 조회하고 updatedAt 갱신을 위해 저장(선점)한다")
    fun claimPendingPayments_ShouldQueryAndSaveToClaim() {
        // given
        given(paymentReader.claimPendingPayments(20)).willReturn(listOf(payment))

        // when
        val claimed = processor.claimPendingPayments(20)

        // then
        assertThat(claimed).hasSize(1)
        verify(paymentReader).claimPendingPayments(20)
    }

    @Test
    @DisplayName("handleRetry 호출 시 재시도 횟수를 증가시키고 한도 미만이면 상태를 유지한 채 저장한다")
    fun handleRetry_WhenUnderLimit_ShouldIncreaseRetryAndSave() {
        // when
        processor.handleRetry(payment, 5)

        // then
        assertThat(payment.retryCount).isEqualTo(1)
        assertThat(payment.state).isEqualTo(PaymentState.PENDING_PG)
        verify(paymentManager).save(payment)
    }

    @Test
    @DisplayName("handleRetry 호출 시 재시도 횟수가 한도에 도달하면 FAILED 상태로 전이 후 저장한다")
    fun handleRetry_WhenLimitReached_ShouldMarkAsFailedAndSave() {
        // given
        val retryExceededPayment = Payment(
            id = 2L,
            principal = Principal.user("U1"),
            orderKey = "ORDER-2",
            originalAmount = BigDecimal.valueOf(10000),
            issuedCouponId = null,
            couponDiscount = BigDecimal.ZERO,
            amount = BigDecimal.valueOf(10000),
            state = PaymentState.PENDING_PG,
            externalPaymentKey = "PAY-KEY-2",
            method = PaymentMethod.CARD,
            paidAt = null,
            approveCode = null,
            createdAt = LocalDateTime.now().minusMinutes(10),
            retryCount = 4 // 4회 실패 상태
        )

        // when
        processor.handleRetry(retryExceededPayment, 5)

        // then
        assertThat(retryExceededPayment.retryCount).isEqualTo(5)
        assertThat(retryExceededPayment.state).isEqualTo(PaymentState.FAILED)
        verify(paymentManager).save(retryExceededPayment)
    }

    @Test
    @DisplayName("handleExpireFail 호출 시 결제 상태를 FAILED로 전이 후 저장한다")
    fun handleExpireFail_ShouldMarkAsFailedAndSave() {
        // when
        processor.handleExpireFail(payment)

        // then
        assertThat(payment.state).isEqualTo(PaymentState.FAILED)
        verify(paymentManager).save(payment)
    }
}
