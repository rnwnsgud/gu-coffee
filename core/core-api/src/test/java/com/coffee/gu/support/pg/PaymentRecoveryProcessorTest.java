package com.coffee.gu.support.pg;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.PaymentMethod;
import com.coffee.gu.enums.PaymentState;
import com.coffee.gu.payment.Payment;
import com.coffee.gu.payment.PaymentManager;
import com.coffee.gu.payment.PaymentReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentRecoveryProcessorTest {

    @Mock
    private PaymentReader paymentReader;

    @Mock
    private PaymentManager paymentManager;

    @InjectMocks
    private PaymentRecoveryProcessor processor;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = new Payment(
                1L,
                Principal.user("U1"),
                "ORDER-1",
                BigDecimal.valueOf(10000),
                null,
                BigDecimal.ZERO,
                BigDecimal.valueOf(10000),
                PaymentState.PENDING_PG,
                "PAY-KEY-1",
                PaymentMethod.CARD,
                null,
                null,
                LocalDateTime.now().minusMinutes(10),
                0
        );
    }

    @Test
    @DisplayName("claimPendingPayments 호출 시 SKIP LOCKED 대상을 조회하고 updatedAt 갱신을 위해 저장(선점)한다")
    void claimPendingPayments_ShouldQueryAndSaveToClaim() {
        // given
        given(paymentReader.claimPendingPayments(20)).willReturn(List.of(payment));

        // when
        List<Payment> claimed = processor.claimPendingPayments(20);

        // then
        assertThat(claimed).hasSize(1);
        verify(paymentReader).claimPendingPayments(20);
    }

    @Test
    @DisplayName("handleRetry 호출 시 재시도 횟수를 증가시키고 한도 미만이면 상태를 유지한 채 저장한다")
    void handleRetry_WhenUnderLimit_ShouldIncreaseRetryAndSave() {
        // when
        processor.handleRetry(payment, 5);

        // then
        assertThat(payment.getRetryCount()).isEqualTo(1);
        assertThat(payment.getState()).isEqualTo(PaymentState.PENDING_PG);
        verify(paymentManager).save(payment);
    }

    @Test
    @DisplayName("handleRetry 호출 시 재시도 횟수가 한도에 도달하면 FAILED 상태로 전이 후 저장한다")
    void handleRetry_WhenLimitReached_ShouldMarkAsFailedAndSave() {
        // given
        Payment retryExceededPayment = new Payment(
                2L,
                Principal.user("U1"),
                "ORDER-2",
                BigDecimal.valueOf(10000),
                null,
                BigDecimal.ZERO,
                BigDecimal.valueOf(10000),
                PaymentState.PENDING_PG,
                "PAY-KEY-2",
                PaymentMethod.CARD,
                null,
                null,
                LocalDateTime.now().minusMinutes(10),
                4 // 4회 실패 상태
        );

        // when
        processor.handleRetry(retryExceededPayment, 5);

        // then
        assertThat(retryExceededPayment.getRetryCount()).isEqualTo(5);
        assertThat(retryExceededPayment.getState()).isEqualTo(PaymentState.FAILED);
        verify(paymentManager).save(retryExceededPayment);
    }

    @Test
    @DisplayName("handleExpireFail 호출 시 결제 상태를 FAILED로 전이 후 저장한다")
    void handleExpireFail_ShouldMarkAsFailedAndSave() {
        // when
        processor.handleExpireFail(payment);

        // then
        assertThat(payment.getState()).isEqualTo(PaymentState.FAILED);
        verify(paymentManager).save(payment);
    }
}
