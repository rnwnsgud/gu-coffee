package com.coffee.gu.payment

import com.coffee.gu.Principal
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import jakarta.persistence.EntityManager
import jakarta.persistence.TransactionRequiredException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.transaction.IllegalTransactionStateException
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest
class JpaExecuteUpdateTransactionTest {

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Test
    @DisplayName("순수 JPA: 트랜잭션 없이 executeUpdate() 호출 시 TransactionRequiredException 발생 검증")
    fun executeUpdate_WithoutTransaction_ShouldThrowTransactionRequiredException() {
        assertThatThrownBy {
            entityManager.createQuery("UPDATE PaymentEntity p SET p.retryCount = p.retryCount + 1")
                .executeUpdate()
        }.isInstanceOf(TransactionRequiredException::class.java)
            .hasMessageContaining("No active transaction for update or delete query")
    }

    @Test
    @DisplayName("Querydsl / Repository: 트랜잭션 없이 벌크 update(.execute())를 실행하는 claimPendingPayments 호출 시 예외 발생 검증")
    fun claimPendingPayments_WithoutTransaction_ShouldThrowException() {
        val payment = Payment(
            id = 0L,
            principal = Principal.user("U-TEST-1"),
            orderKey = "ORDER-TX-TEST",
            originalAmount = BigDecimal.valueOf(10000),
            issuedCouponId = null,
            couponDiscount = null,
            amount = BigDecimal.valueOf(10000),
            state = PaymentState.PENDING_PG,
            externalPaymentKey = "PAY-KEY-TX",
            method = PaymentMethod.CARD,
            paidAt = null,
            approveCode = null,
            createdAt = LocalDateTime.now().minusMinutes(10),
            retryCount = 0
        )
        paymentRepository.save(payment)

        assertThatThrownBy {
            paymentRepository.claimPendingPayments(10)
        }.isInstanceOfAny(
            IllegalTransactionStateException::class.java,
            TransactionRequiredException::class.java,
            InvalidDataAccessApiUsageException::class.java
        )
    }
}
