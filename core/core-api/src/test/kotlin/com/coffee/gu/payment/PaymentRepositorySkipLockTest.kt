package com.coffee.gu.payment

import com.coffee.gu.Principal
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PaymentRepositorySkipLockTest {

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update("DELETE FROM payment")
    }

    @Test
    @DisplayName("claimPendingPayments 호출 시 다른 트랜잭션이 락을 쥔 레코드는 대기 없이 스킵(SKIP LOCKED)한다")
    fun testGetPendingPaymentsSkipLock() {
        // given
        val p1 = Payment(
            id = 0L, principal = Principal.user("U1"), orderKey = "ORDER-SKIP-1",
            originalAmount = BigDecimal.valueOf(10000), issuedCouponId = null, couponDiscount = null,
            amount = BigDecimal.valueOf(10000), state = PaymentState.PENDING_PG,
            externalPaymentKey = "PAY-KEY-1", method = PaymentMethod.CARD,
            paidAt = null, approveCode = null, createdAt = LocalDateTime.now().minusMinutes(10), retryCount = 0
        )
        val p2 = Payment(
            id = 0L, principal = Principal.user("U2"), orderKey = "ORDER-SKIP-2",
            originalAmount = BigDecimal.valueOf(20000), issuedCouponId = null, couponDiscount = null,
            amount = BigDecimal.valueOf(20000), state = PaymentState.PENDING_PG,
            externalPaymentKey = "PAY-KEY-2", method = PaymentMethod.CARD,
            paidAt = null, approveCode = null, createdAt = LocalDateTime.now().minusMinutes(10), retryCount = 0
        )

        paymentRepository.save(p1)
        paymentRepository.save(p2)

        val executor = Executors.newFixedThreadPool(2)
        val tx1LockedLatch = CountDownLatch(1)
        val tx2FinishLatch = CountDownLatch(1)

        // Tx1: claimPendingPayments()를 통해 P1, P2 레코드에 FOR UPDATE SKIP LOCKED 락을 잡음
        val tx1Future = executor.submit<List<Payment>> {
            val status = transactionManager.getTransaction(DefaultTransactionDefinition())
            val lockedPayments = paymentRepository.claimPendingPayments(2)
            tx1LockedLatch.countDown()

            tx2FinishLatch.await(5, TimeUnit.SECONDS)
            transactionManager.rollback(status)
            lockedPayments
        }

        tx1LockedLatch.await(3, TimeUnit.SECONDS)

        // Tx2: 다른 트랜잭션에서 claimPendingPayments() 실행 -> 대기 없이 Tx1이 잡은 레코드를 스킵
        val startTime = System.currentTimeMillis()
        val tx2Future = executor.submit<List<Payment>> {
            val status = transactionManager.getTransaction(DefaultTransactionDefinition())
            val result = paymentRepository.claimPendingPayments(2)
            transactionManager.commit(status)
            result
        }

        val tx2Result = tx2Future.get(3, TimeUnit.SECONDS)
        val elapsedTime = System.currentTimeMillis() - startTime

        tx2FinishLatch.countDown()
        tx1Future.get()
        executor.shutdown()

        // then
        assertThat(elapsedTime).isLessThan(2000)
        assertThat(tx2Result).isEmpty()
    }
}
