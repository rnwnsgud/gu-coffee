package com.coffee.gu.cancel;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.Principal;
import com.coffee.gu.enums.OrderState;
import com.coffee.gu.enums.PaymentState;
import com.coffee.gu.order.Order;
import com.coffee.gu.payment.Payment;
import com.coffee.gu.stamp.StampRevertManager;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CancelValidatorTest {

    @Mock
    private StampRevertManager stampRevertManager;

    @InjectMocks
    private CancelValidator cancelValidator;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order(
                "ORDER-VAL-1",
                "아메리카노",
                Principal.user("U1"),
                1L,
                BigDecimal.valueOf(3000),
                OrderState.PAID,
                List.of()
        );
    }

    @Test
    @DisplayName("결제 상태가 SUCCESS인 일반 취소 대상은 검증을 통과한다")
    void validate_WhenPaymentIsSuccess_ShouldPass() {
        // given
        Payment payment = new Payment(
                1L,
                Principal.user("U1"),
                "ORDER-VAL-1",
                BigDecimal.valueOf(3000),
                null,
                BigDecimal.ZERO,
                BigDecimal.valueOf(3000),
                PaymentState.SUCCESS,
                "PAY-KEY-SUCCESS",
                null,
                null,
                null,
                LocalDateTime.now(),
                0
        );

        // when & then
        assertThatCode(() -> cancelValidator.validate(order, payment))
                .doesNotThrowAnyException();
        verify(stampRevertManager).validateRevertable(order);
    }

    @Test
    @DisplayName("결제 상태가 FAILED이지만 externalPaymentKey가 존재하는 보상 취소 대상은 검증을 통과한다")
    void validate_WhenPaymentIsFailedWithExternalPaymentKey_ShouldPass() {
        // given
        Payment payment = new Payment(
                1L,
                Principal.user("U1"),
                "ORDER-VAL-1",
                BigDecimal.valueOf(3000),
                null,
                BigDecimal.ZERO,
                BigDecimal.valueOf(3000),
                PaymentState.FAILED,
                "PAY-KEY-COMPENSATE",
                null,
                null,
                null,
                LocalDateTime.now(),
                0
        );

        // when & then
        assertThatCode(() -> cancelValidator.validate(order, payment))
                .doesNotThrowAnyException();
        verify(stampRevertManager).validateRevertable(order);
    }

    @Test
    @DisplayName("결제 상태가 PENDING_PG이거나 READY인 미승인 건은 취소 검증에서 예외를 던진다")
    void validate_WhenPaymentIsPendingOrReady_ShouldThrowException() {
        // given
        Payment pendingPayment = new Payment(
                1L,
                Principal.user("U1"),
                "ORDER-VAL-1",
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

        // when & then
        assertThatThrownBy(() -> cancelValidator.validate(order, pendingPayment))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.PAYMENT_INVALID_STATE);
    }

    @Test
    @DisplayName("결제 상태가 FAILED이지만 externalPaymentKey가 없는 건은 취소 검증에서 예외를 던진다")
    void validate_WhenPaymentIsFailedWithoutExternalPaymentKey_ShouldThrowException() {
        // given
        Payment failedPaymentWithoutKey = new Payment(
                1L,
                Principal.user("U1"),
                "ORDER-VAL-1",
                BigDecimal.valueOf(3000),
                null,
                BigDecimal.ZERO,
                BigDecimal.valueOf(3000),
                PaymentState.FAILED,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                0
        );

        // when & then
        assertThatThrownBy(() -> cancelValidator.validate(order, failedPaymentWithoutKey))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.PAYMENT_INVALID_STATE);
    }
}
