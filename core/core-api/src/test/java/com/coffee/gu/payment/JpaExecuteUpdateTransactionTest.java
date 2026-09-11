package com.coffee.gu.payment;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.PaymentMethod;
import com.coffee.gu.enums.PaymentState;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TransactionRequiredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class JpaExecuteUpdateTransactionTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("순수 JPA: 트랜잭션 없이 executeUpdate() 호출 시 TransactionRequiredException 발생 검증")
    void executeUpdate_WithoutTransaction_ShouldThrowTransactionRequiredException() {
        // given & when & then
        // 트랜잭션(@Transactional)이 없는 환경에서 executeUpdate()를 실행하면 즉시 TransactionRequiredException 발생
        assertThatThrownBy(() -> {
            entityManager.createQuery("UPDATE PaymentEntity p SET p.retryCount = p.retryCount + 1")
                    .executeUpdate();
        }).isInstanceOf(TransactionRequiredException.class)
          .hasMessageContaining("No active transaction for update or delete query");
    }

    @Test
    @DisplayName("Querydsl / Repository: 트랜잭션 없이 벌크 update(.execute())를 실행하는 claimPendingPayments 호출 시 예외 발생 검증")
    void claimPendingPayments_WithoutTransaction_ShouldThrowException() {
        // given: 5분 전 결제 데이터 적재
        Payment payment = new Payment(
                0L,
                Principal.user("U-TEST-1"),
                "ORDER-TX-TEST",
                BigDecimal.valueOf(10000),
                null,
                BigDecimal.ZERO,
                BigDecimal.valueOf(10000),
                PaymentState.PENDING_PG,
                "PAY-KEY-TX",
                PaymentMethod.CARD,
                null,
                null,
                LocalDateTime.now().minusMinutes(10),
                0
        );
        paymentRepository.save(payment);

        // when & then
        // @Transactional 없이 claimPendingPayments를 호출하면 트랜잭션 부재 예외(IllegalTransactionStateException 또는 TransactionRequiredException) 발생
        assertThatThrownBy(() -> {
            paymentRepository.claimPendingPayments(10);
        }).isInstanceOfAny(
                org.springframework.transaction.IllegalTransactionStateException.class,
                TransactionRequiredException.class,
                InvalidDataAccessApiUsageException.class
        );
    }
}
