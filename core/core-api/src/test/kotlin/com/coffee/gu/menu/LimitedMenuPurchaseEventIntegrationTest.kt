package com.coffee.gu.menu

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.PGConfirmResult
import com.coffee.gu.PGPayment
import com.coffee.gu.PaymentGateway
import com.coffee.gu.PaymentGatewayStatus
import com.coffee.gu.Principal
import com.coffee.gu.TestApplication
import com.coffee.gu.cancel.CancelService
import com.coffee.gu.enums.MenuType
import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.enums.StoreStatus
import com.coffee.gu.lock.LimitedMenuStockReservationManager
import com.coffee.gu.order.NewOrder
import com.coffee.gu.order.NewOrderLine
import com.coffee.gu.order.OrderService
import com.coffee.gu.payment.PaymentDiscount
import com.coffee.gu.payment.PaymentRepository
import com.coffee.gu.payment.PaymentService
import com.coffee.gu.store.BusinessInformation
import com.coffee.gu.store.SalesInformation
import com.coffee.gu.store.Store
import com.coffee.gu.store.StoreLocation
import com.coffee.gu.store.StoreRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(classes = [TestApplication::class])
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class LimitedMenuPurchaseEventIntegrationTest {

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var paymentService: PaymentService

    @Autowired
    private lateinit var cancelService: CancelService

    @Autowired
    private lateinit var menuFinder: MenuFinder

    @Autowired
    private lateinit var limitedMenuManager: LimitedMenuManager

    @Autowired
    private lateinit var limitedMenuFinder: LimitedMenuFinder

    @Autowired
    private lateinit var limitedMenuStockManager: LimitedMenuStockManager

    @Autowired
    private lateinit var reservationManager: LimitedMenuStockReservationManager

    @Autowired
    private lateinit var menuJpaRepository: MenuJpaRepository

    @Autowired
    private lateinit var storeRepository: StoreRepository

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @MockitoBean
    private lateinit var paymentGateway: PaymentGateway

    private var storeId: Long = 0L
    private var limitedMenuId: Long = 0L
    private val menuPrice = BigDecimal("45000.00")

    @BeforeEach
    fun setUp() {
        val location = StoreLocation("서울 강남구", 37.5, 127.0)
        val salesInfo = SalesInformation(location, emptyList(), "02-123-4567")
        val busiInfo = BusinessInformation("홍길동", "구커피 강남점", "123-45-67890", "서울 강남구")
        val savedStore = storeRepository.save(Store(0L, "강남점", "GN01", StoreStatus.OPEN, salesInfo, busiInfo))
        storeId = savedStore.id

        val collaboratorTumbler = MenuEntity(
            name = "한정판 콜라보 텀블러",
            type = MenuType.MD,
            costPrice = BigDecimal("20000.00"),
            salesPrice = menuPrice,
            description = "선착순 한정판 MD",
        )
        collaboratorTumbler.active()
        val savedMenu = menuJpaRepository.saveAndFlush(collaboratorTumbler)
        limitedMenuId = savedMenu.id
    }

    private fun registerLimitedMenu(totalQuantity: Int, maxPurchasePerUser: Int = 1): LimitedMenu {
        val targetMenu = menuFinder.getById(limitedMenuId)
        val createdLimitedMenu = limitedMenuManager.create(
            LimitedMenu(
                menu = targetMenu,
                totalQuantity = totalQuantity,
                soldQuantity = 0,
                maxPurchasePerUser = maxPurchasePerUser,
            )
        )
        limitedMenuStockManager.initializeStock(limitedMenuId, totalQuantity)
        return createdLimitedMenu
    }

    @Test
    @DisplayName("시나리오 1: 한정 수량 10개인 MD 메뉴에 100명이 동시에 주문 요청 시 정확히 10명만 재고 선점에 성공하고 90명은 LIMITED_MENU_SOLD_OUT으로 즉시 차단된다")
    fun concurrentOrderReservation_Only10SucceedAnd90FailWithSoldOut() {
        // given
        registerLimitedMenu(totalQuantity = 10)
        val threadCount = 100
        val executor = Executors.newFixedThreadPool(32)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        val reservedSuccessCount = AtomicInteger(0)
        val soldOutFailCount = AtomicInteger(0)

        // when
        for (i in 1..threadCount) {
            executor.submit {
                try {
                    startLatch.await()
                    val principal = Principal.user("USER-$i")
                    val newOrder = NewOrder(
                        principal = principal,
                        storeId = storeId,
                        lines = listOf(NewOrderLine(limitedMenuId, 1L)),
                    )
                    orderService.create(newOrder)
                    reservedSuccessCount.incrementAndGet()
                } catch (e: CoreException) {
                    if (e.errorType == ErrorType.LIMITED_MENU_SOLD_OUT) {
                        soldOutFailCount.incrementAndGet()
                    }
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        // then
        assertThat(reservedSuccessCount.get()).isEqualTo(10)
        assertThat(soldOutFailCount.get()).isEqualTo(90)
        assertThat(limitedMenuStockManager.getAvailableStock(limitedMenuId)).isEqualTo(0)
    }

    @Test
    @DisplayName("시나리오 2 (2단계 결제 사전 차단): User A의 선점 TTL이 만료되면 User B가 실시간 회수하여 결제에 성공하고, 뒤늦게 돌아온 User A는 외부 PG 승인 호출 자체가 차단(LIMITED_MENU_RESERVATION_EXPIRED)된다")
    fun whenReservationTtlExpires_NextUserClaimsAndOriginalUserIsBlockedBeforePgConfirm() {
        // given: 마지막 1개 남은 한정판 메뉴
        registerLimitedMenu(totalQuantity = 1)
        val userA = Principal.user("USER-A")
        val userB = Principal.user("USER-B")

        // 1. User A가 주문 생성 및 결제 준비(READY)까지 완료 (재고 1 -> 0 선점)
        val orderKeyA = orderService.create(
            NewOrder(userA, storeId, listOf(NewOrderLine(limitedMenuId, 1L)))
        )
        val orderA = orderService.getOrder(orderKeyA)
        paymentService.createPayment(orderA, PaymentDiscount.of(emptyList(), null, menuPrice))
        assertThat(limitedMenuStockManager.getAvailableStock(limitedMenuId)).isEqualTo(0)

        // 2. User A가 결제창에서 시간을 지체하여 선점 TTL(3분) 만료
        (reservationManager as LocalLimitedMenuStockReservationManager).forceExpireOrder(limitedMenuId, orderKeyA)

        // 3. 스케줄러 대기 없이 User B가 주문 생성 시 ZSET Lazy Cleanup으로 즉시 재고를 회수하여 선점 및 결제 완료
        val orderKeyB = orderService.create(
            NewOrder(userB, storeId, listOf(NewOrderLine(limitedMenuId, 1L)))
        )
        val orderB = orderService.getOrder(orderKeyB)
        paymentService.createPayment(orderB, PaymentDiscount.of(emptyList(), null, menuPrice))

        given(paymentGateway.getByOrderKey(orderKeyB))
            .willReturn(PGPayment("PG-KEY-B", orderKeyB, menuPrice, PaymentGatewayStatus.IN_PROGRESS))
        given(paymentGateway.confirm(any()))
            .willReturn(PGConfirmResult.success(orderKeyB, "PG-KEY-B", PaymentMethod.CARD, "APPR-B", OffsetDateTime.now()))

        val approvalResultB = paymentService.approvePayment(orderB)
        assertThat(approvalResultB.paymentState).isEqualTo(PaymentState.SUCCESS)
        assertThat(limitedMenuFinder.getByMenuId(limitedMenuId).soldQuantity).isEqualTo(1)

        // 4. 뒤늦게 결제창 인증을 마치고 돌아온 User A가 최종 결제 승인을 요청
        given(paymentGateway.getByOrderKey(orderKeyA))
            .willReturn(PGPayment("PG-KEY-A", orderKeyA, menuPrice, PaymentGatewayStatus.IN_PROGRESS))

        // when & then: PG사 confirm API는 호출조차 되지 않고 LIMITED_MENU_RESERVATION_EXPIRED로 사전 차단됨
        assertThatThrownBy {
            paymentService.approvePayment(orderA)
        }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.LIMITED_MENU_RESERVATION_EXPIRED)

        // PG confirm은 오직 User B의 1회만 호출되었고 User A에 대해서는 호출되지 않았음을 검증
        verify(paymentGateway, times(1)).confirm(any())
        verify(paymentGateway, never()).cancel(any())

        val paymentA = paymentRepository.findByOrderKey(orderKeyA)
        assertThat(paymentA.state).isEqualTo(PaymentState.FAILED)
        assertThat(limitedMenuFinder.getByMenuId(limitedMenuId).soldQuantity).isEqualTo(1)
    }

    @Test
    @DisplayName("시나리오 3 (1단계 자동 승인 PG 사후 자동 환불): 선점 TTL이 만료된 뒤 이미 PG에서 승인 완료(DONE)된 상태로 도착하면, 즉시 PG 취소(cancel)를 호출해 자동 환불하고 실패 시 Outbox로 보상한다")
    fun whenOneClickPgAlreadyApprovedAfterTtlExpiry_ShouldImmediatelyAutoCancelPayment() {
        // given: 마지막 1개 남은 한정판 메뉴
        registerLimitedMenu(totalQuantity = 1)
        val userA = Principal.user("USER-A")

        val orderKeyA = orderService.create(
            NewOrder(userA, storeId, listOf(NewOrderLine(limitedMenuId, 1L)))
        )
        val orderA = orderService.getOrder(orderKeyA)
        paymentService.createPayment(orderA, PaymentDiscount.of(emptyList(), null, menuPrice))

        // User A의 선점 TTL이 만료됨
        (reservationManager as LocalLimitedMenuStockReservationManager).forceExpireOrder(limitedMenuId, orderKeyA)

        // 하지만 User A가 사용한 1단계 원클릭 PG에서는 이미 결제가 DONE 상태로 승인되어 인입됨
        given(paymentGateway.getByOrderKey(orderKeyA))
            .willReturn(PGPayment("PG-KEY-AUTO-A", orderKeyA, menuPrice, PaymentGatewayStatus.DONE))

        // when & then: 즉시 PG 자동 취소가 호출되고 LIMITED_MENU_OVERSOLD_AUTO_CANCELED 예외가 발생함
        assertThatThrownBy {
            paymentService.approvePayment(orderA)
        }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.LIMITED_MENU_OVERSOLD_AUTO_CANCELED)

        verify(paymentGateway, times(1)).cancel(any())
        verify(paymentGateway, never()).confirm(any())

        val paymentA = paymentRepository.findByOrderKey(orderKeyA)
        assertThat(paymentA.state).isEqualTo(PaymentState.FAILED)
        assertThat(limitedMenuFinder.getByMenuId(limitedMenuId).soldQuantity).isEqualTo(0)
        assertThat(limitedMenuStockManager.getAvailableStock(limitedMenuId)).isEqualTo(1)
    }

    @Test
    @DisplayName("시나리오 4: 결제 완료 후 주문을 취소하면 DB soldQuantity와 Redis 가용 재고가 모두 즉시 복구되어 다음 유저가 구매할 수 있다")
    fun whenPaidLimitedMenuOrderIsCanceled_StockAndSoldQuantityAreRestored() {
        // given: 1개 한정판 메뉴를 User A가 주문 및 결제 완료
        registerLimitedMenu(totalQuantity = 1)
        val userA = Principal.user("USER-A")
        val userB = Principal.user("USER-B")

        val orderKeyA = orderService.create(
            NewOrder(userA, storeId, listOf(NewOrderLine(limitedMenuId, 1L)))
        )
        val orderA = orderService.getOrder(orderKeyA)
        paymentService.createPayment(orderA, PaymentDiscount.of(emptyList(), null, menuPrice))

        given(paymentGateway.getByOrderKey(orderKeyA))
            .willReturn(PGPayment("PG-KEY-A", orderKeyA, menuPrice, PaymentGatewayStatus.IN_PROGRESS))
        given(paymentGateway.confirm(any()))
            .willReturn(PGConfirmResult.success(orderKeyA, "PG-KEY-A", PaymentMethod.CARD, "APPR-A", OffsetDateTime.now()))

        paymentService.approvePayment(orderA)
        assertThat(limitedMenuFinder.getByMenuId(limitedMenuId).soldQuantity).isEqualTo(1)
        assertThat(limitedMenuStockManager.getAvailableStock(limitedMenuId)).isEqualTo(0)

        // when: User A가 결제 취소 요청
        val paidOrderA = orderService.getOrder(orderKeyA, OrderState.PAID)
        cancelService.cancel(paidOrderA)

        // then: DB soldQuantity가 0으로 감소하고, 가용 재고가 1로 복구되어 User B가 즉시 주문 가능
        assertThat(limitedMenuFinder.getByMenuId(limitedMenuId).soldQuantity).isEqualTo(0)
        assertThat(limitedMenuStockManager.getAvailableStock(limitedMenuId)).isEqualTo(1)

        val orderKeyB = orderService.create(
            NewOrder(userB, storeId, listOf(NewOrderLine(limitedMenuId, 1L)))
        )
        assertThat(orderKeyB).isNotBlank()
        assertThat(limitedMenuStockManager.getAvailableStock(limitedMenuId)).isEqualTo(0)
    }
}
