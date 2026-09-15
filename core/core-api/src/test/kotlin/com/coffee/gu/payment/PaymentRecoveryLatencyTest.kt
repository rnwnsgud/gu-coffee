package com.coffee.gu.payment

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
import com.coffee.gu.support.pg.PaymentRecoverScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PaymentRecoveryLatencyTest {

    @Autowired
    private lateinit var paymentRecoverScheduler: PaymentRecoverScheduler

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var storeRepository: StoreRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @MockitoBean
    private lateinit var paymentGateway: PaymentGateway

    @Test
    @DisplayName("복구 대상 건수별(targetCount) 실제 1건당 평균 처리 시간(Latency) 측정")
    fun measurePaymentRecoveryLatency() {
        // given
        val targetCount = 72
        val prefix = UUID.randomUUID().toString().substring(0, 8)

        val location = StoreLocation("인천 부평구", 37.5, 127.0)
        val salesInfo = SalesInformation(location, emptyList(), "032-123-4567")
        val busiInfo = BusinessInformation("홍길동", "구커피 부평점", "123-45-67890", "인천 부평구")
        val store = storeRepository.save(Store(0L, "부평점", "BP01", StoreStatus.OPEN, salesInfo, busiInfo))

        for (i in 0 until targetCount) {
            val orderKey = "ORDER-LATENCY-$prefix-$i"
            val user = Principal.user("U-$prefix-$i")
            val amount = BigDecimal("3000.00")

            val line = OrderLine(null, orderKey, store.id, "아메리카노", null, null, 1L, amount, amount, false)
            val order = Order(orderKey, "아메리카노 1잔", user, store.id, amount, OrderState.CREATED, listOf(line))
            orderRepository.create(order)

            paymentRepository.save(
                Payment(
                    id = 0L,
                    principal = user,
                    orderKey = orderKey,
                    originalAmount = amount,
                    issuedCouponId = null,
                    couponDiscount = null,
                    amount = amount,
                    state = PaymentState.PENDING_PG,
                    externalPaymentKey = "PAY-KEY-$prefix-$i",
                    method = PaymentMethod.CARD,
                    paidAt = null,
                    approveCode = null,
                    createdAt = LocalDateTime.now().minusMinutes(10),
                    retryCount = 0
                )
            )
        }

        // updatedAt을 5분 이전으로 세팅 (Auditing 우회)
        jdbcTemplate.update("UPDATE payment SET updated_at = ?", Timestamp.valueOf(LocalDateTime.now().minusMinutes(10)))

        // PG사 외부 REST API 네트워크 RTT (실무 카드사/VAN사 통신 포함 평균 200ms 지연) Mocking
        given(paymentGateway.getByOrderKey(any()))
            .willAnswer { invocation ->
                Thread.sleep(200)
                val orderKey = invocation.getArgument<String>(0)
                PGPayment("PAY-KEY-100", orderKey, BigDecimal("3000.00"), PaymentGatewayStatus.DONE)
            }

        given(paymentGateway.confirm(any()))
            .willReturn(PGConfirmResult.success("ORDER-KEY", "PAY-KEY-100", PaymentMethod.CARD, "APPROVE-100", OffsetDateTime.now()))

        // when
        val totalStartTime = System.nanoTime()

        paymentRecoverScheduler.schedule()

        val totalElapsedTimeMs = (System.nanoTime() - totalStartTime) / 1_000_000

        // then
        val actualProcessedCount = PaymentRecoverScheduler.LIMIT
        val avgLatencyPerItem = totalElapsedTimeMs.toDouble() / actualProcessedCount

        // 1분(60,000ms) 스케줄러 주기 내에서 단일 스레드가 소화 가능한 이론상 최대 건수 (안전 마진 80% 기준)
        val maxSafeCapacityPerMinute = (60_000 * 0.8) / avgLatencyPerItem

        println("====== [결제 복구 Latency & 스케줄러 용량 실측 결과] ======")
        println("전체 백로그 건수: $targetCount 건")
        println("1회 스케줄러 처리 건수: $actualProcessedCount 건 (LIMIT 기준)")
        println("1회 스케줄러 총 소요 시간: $totalElapsedTimeMs ms")
        println("1건당 평균 처리 시간(네트워크 RTT 200ms 포함): ${String.format("%.2f", avgLatencyPerItem)} ms")
        println("1분 스케줄러 주기 내 안전 처리 한계(Capacity Limit): 최대 ${maxSafeCapacityPerMinute.toInt()} 건")
        println("=====================================================")

        // 1건당 평균 처리 시간은 네트워크 지연(200ms) 이상이어야 함 (왜곡 제거 검증)
        assertThat(avgLatencyPerItem).isGreaterThanOrEqualTo(200.0)

        // 현재 설정된 LIMIT(20건)은 1분 주기의 20% 이내(12초 미만)로 안전하게 수행됨을 검증
        assertThat(totalElapsedTimeMs).isLessThan(12_000)
    }
}
