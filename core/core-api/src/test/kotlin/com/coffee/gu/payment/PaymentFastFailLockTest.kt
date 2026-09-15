package com.coffee.gu.payment

import com.coffee.gu.CoreException
import com.coffee.gu.PGConfirmResult
import com.coffee.gu.PGPayment
import com.coffee.gu.PaymentGateway
import com.coffee.gu.PaymentGatewayConfirm
import com.coffee.gu.PaymentGatewayStatus
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.enums.StoreStatus
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderLine
import com.coffee.gu.order.OrderRepository
import com.coffee.gu.store.BusinessInformation
import com.coffee.gu.store.SalesInformation
import com.coffee.gu.store.Store
import com.coffee.gu.store.StoreLocation
import com.coffee.gu.store.StoreRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.ArrayList
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PaymentFastFailLockTest {

    @Autowired
    private lateinit var paymentService: PaymentService

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var storeRepository: StoreRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @MockitoBean
    private lateinit var paymentGateway: PaymentGateway

    @Test
    @DisplayName("동시 10개 요청 진입 시 비관적 락 기반 Fast-Fail로 외부 PG 승인은 단 1회만 호출되고 중복 호출은 차단된다")
    fun testConcurrentPaymentApprovalFastFail() {
        // given
        val orderKey = "ORDER-FAST-FAIL-" + UUID.randomUUID()
        val user = Principal.user("U100")
        val amount = BigDecimal("3000.00")

        val location = StoreLocation("인천 부평구", 37.5, 127.0)
        val salesInfo = SalesInformation(location, emptyList(), "032-123-4567")
        val busiInfo = BusinessInformation("홍길동", "구커피 부평점", "123-45-67890", "인천 부평구")

        val store = storeRepository.save(Store(0L, "부평점", "BP01", StoreStatus.OPEN, salesInfo, busiInfo))
        val storeId = store.id

        val line = OrderLine(null, orderKey, storeId, "아메리카노", null, null, 1L, amount, amount, false)
        val order = Order(orderKey, "아메리카노 1잔", user, storeId, amount, OrderState.CREATED, listOf(line))
        orderRepository.create(order)

        paymentService.createPayment(order, PaymentDiscount.of(emptyList(), null, amount))

        val dbPayment = paymentRepository.findByOrderKey(orderKey)
        val exactAmount = dbPayment.amount

        // PG Mock 설정
        given(paymentGateway.getByOrderKey(orderKey))
            .willReturn(PGPayment("PAY-KEY-100", orderKey, exactAmount, PaymentGatewayStatus.READY))

        given(paymentGateway.confirm(any()))
            .willReturn(PGConfirmResult.success(orderKey, "PAY-KEY-100", PaymentMethod.CARD, "APPROVE-100", OffsetDateTime.now()))

        val threadCount = 10
        val executorService = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val endLatch = CountDownLatch(threadCount)

        val futures = ArrayList<Future<PaymentApprovalResult>>()
        val successCount = AtomicInteger(0)
        val fastFailCount = AtomicInteger(0)

        // when
        for (i in 0 until threadCount) {
            futures.add(executorService.submit<PaymentApprovalResult> {
                startLatch.await()
                try {
                    paymentService.approvePayment(order)
                } finally {
                    endLatch.countDown()
                }
            })
        }

        startLatch.countDown() // 10개 스레드 동시에 출발
        endLatch.await()
        executorService.shutdown()

        for (future in futures) {
            try {
                val result = future.get()
                if (result.paymentState == PaymentState.SUCCESS) {
                    successCount.incrementAndGet()
                }
            } catch (e: ExecutionException) {
                if (e.cause is CoreException) {
                    fastFailCount.incrementAndGet()
                }
            }
        }

        // then
        assertThat(futures.size).isEqualTo(10)
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1)

        // 외부 PG 승인(confirm) API 호출은 오직 단 1회만 실행되어야 함
        verify(paymentGateway, times(1)).confirm(any())

        // 최종 DB 결제 상태는 SUCCESS 이어야 함
        entityManager.clear()
        val finalPayment = paymentRepository.findByOrderKey(orderKey)!!
        assertThat(finalPayment.isPaid).isTrue()
    }
}
