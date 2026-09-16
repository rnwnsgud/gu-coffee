package com.coffee.gu.payment

import com.coffee.gu.PGConfirmResult
import com.coffee.gu.PGPayment
import com.coffee.gu.PaymentGateway
import com.coffee.gu.PaymentGatewayStatus
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.enums.StoreStatus
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderLine
import com.coffee.gu.order.OrderRepository
import com.coffee.gu.stamp.StampHandler
import com.coffee.gu.stamp.StampRepository
import com.coffee.gu.store.BusinessInformation
import com.coffee.gu.store.SalesInformation
import com.coffee.gu.store.Store
import com.coffee.gu.store.StoreLocation
import com.coffee.gu.store.StoreRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PaymentStampEventIsolationTest {

    @Autowired
    private lateinit var paymentService: PaymentService

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var orderLineRepository: com.coffee.gu.order.OrderLineRepository

    @Autowired
    private lateinit var storeRepository: StoreRepository

    @Autowired
    private lateinit var stampRepository: StampRepository

    @MockitoBean
    private lateinit var paymentGateway: PaymentGateway

    @MockitoSpyBean
    private lateinit var stampHandler: StampHandler

    @Test
    @DisplayName("결제 성공 시 스탬프 적립에서 런타임 예외(장애)가 발생하더라도 결제 승인과 주문은 롤백되지 않고 정상 커밋된다 (Fault Isolation)")
    fun paymentApprovalShouldCommitEvenIfStampRewardFails() {
        // given
        val orderKey = "ORDER-ISO-" + UUID.randomUUID()
        val user = Principal.user("USER-ISO-01")
        val amount = BigDecimal("4500.00")

        val location = StoreLocation("서울 강남구", 37.5, 127.0)
        val salesInfo = SalesInformation(location, emptyList(), "02-123-4567")
        val busiInfo = BusinessInformation("김사장", "구커피 강남점", "111-22-33333", "서울 강남구")
        val store = storeRepository.save(Store(0L, "강남점", "GN01", StoreStatus.OPEN, salesInfo, busiInfo))

        // 스탬프 적립 대상 음료 1잔
        val line = OrderLine(null, orderKey, store.id, "카페라떼", null, null, 1L, amount, amount, true)
        orderLineRepository.saveAll(listOf(line))
        val order = Order(orderKey, "카페라떼 1잔", user, store.id, amount, OrderState.CREATED, listOf(line))
        orderRepository.create(order)

        paymentService.createPayment(order, PaymentDiscount.of(emptyList(), null, amount))

        // PG Mock 성공 응답
        given(paymentGateway.getByOrderKey(orderKey))
            .willReturn(PGPayment("PAY-KEY-ISO", orderKey, amount, PaymentGatewayStatus.READY))
        given(paymentGateway.confirm(any()))
            .willReturn(PGConfirmResult.success(orderKey, "PAY-KEY-ISO", PaymentMethod.CARD, "APPROVE-ISO", OffsetDateTime.now()))

        // [핵심 시뮬레이션]: 스탬프 적립 도중 DB Deadlock 또는 런타임 장애 고의 발생
        doThrow(RuntimeException("스탬프 DB 장애 발생 시뮬레이션"))
            .whenever(stampHandler).reward(any())

        // when: 결제 승인 실행
        val result = paymentService.approvePayment(order)

        // then
        // 1. 스탬프 적립에 장애가 났음에도 결제 승인은 성공(SUCCESS)으로 반환되어야 함
        assertThat(result.paymentState).isEqualTo(PaymentState.SUCCESS)

        // 2. 결제 엔티티는 롤백되지 않고 SUCCESS 상태로 DB에 정상 커밋되어야 함
        val committedPayment = paymentRepository.findByOrderKey(orderKey)
        assertThat(committedPayment.state).isEqualTo(PaymentState.SUCCESS)
        assertThat(committedPayment.externalPaymentKey).isEqualTo("PAY-KEY-ISO")

        // 3. 주문 엔티티도 PAID 상태로 정상 커밋되어야 함
        val committedOrder = orderRepository.findByOrderKey(orderKey)
        assertThat(committedOrder.state).isEqualTo(OrderState.PAID)
    }

    @Test
    @DisplayName("정상 결제 승인 완료 시 AFTER_COMMIT 비동기 이벤트를 통해 스탬프가 정상 적립된다")
    fun paymentApprovalShouldTriggerStampRewardAfterCommit() {
        // given
        val orderKey = "ORDER-NORMAL-" + UUID.randomUUID()
        val user = Principal.user("USER-NORMAL-01")
        val amount = BigDecimal("4500.00")

        val location = StoreLocation("서울 강남구", 37.5, 127.0)
        val salesInfo = SalesInformation(location, emptyList(), "02-123-4567")
        val busiInfo = BusinessInformation("김사장", "구커피 강남점", "111-22-33333", "서울 강남구")
        val store = storeRepository.save(Store(0L, "강남점", "GN01", StoreStatus.OPEN, salesInfo, busiInfo))

        // 스탬프 적립 대상 음료 2잔
        val line = OrderLine(null, orderKey, store.id, "카페라떼", null, null, 2L, amount, amount, true)
        orderLineRepository.saveAll(listOf(line))
        val order = Order(orderKey, "카페라떼 2잔", user, store.id, amount, OrderState.CREATED, listOf(line))
        orderRepository.create(order)

        paymentService.createPayment(order, PaymentDiscount.of(emptyList(), null, amount))

        given(paymentGateway.getByOrderKey(orderKey))
            .willReturn(PGPayment("PAY-KEY-NORMAL", orderKey, amount, PaymentGatewayStatus.READY))
        given(paymentGateway.confirm(any()))
            .willReturn(PGConfirmResult.success(orderKey, "PAY-KEY-NORMAL", PaymentMethod.CARD, "APPROVE-NORMAL", OffsetDateTime.now()))

        // when
        val result = paymentService.approvePayment(order)

        // then
        assertThat(result.paymentState).isEqualTo(PaymentState.SUCCESS)

        // 비동기 스탬프 처리 대기 (최대 2초)
        var stamps = stampRepository.findByOrderKey(orderKey)
        val deadline = System.currentTimeMillis() + 2000
        while (System.currentTimeMillis() < deadline && stamps.isEmpty()) {
            Thread.sleep(50)
            stamps = stampRepository.findByOrderKey(orderKey)
        }

        // 스탬프 2개가 해당 유저와 주문번호로 정상 적립되었는지 검증
        assertThat(stamps).hasSize(2)
        assertThat(stamps.all { it.principal.key == user.key }).isTrue()
    }
}
