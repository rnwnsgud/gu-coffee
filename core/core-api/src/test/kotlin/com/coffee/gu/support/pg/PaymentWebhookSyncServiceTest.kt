package com.coffee.gu.support.pg

import com.coffee.gu.CancelEvent
import com.coffee.gu.EventLogRepository
import com.coffee.gu.PGConfirmResult
import com.coffee.gu.Principal
import com.coffee.gu.TransactionHistoryManager
import com.coffee.gu.cancel.Cancel
import com.coffee.gu.cancel.CancelManager
import com.coffee.gu.cancel.CancelRecorder
import com.coffee.gu.cancel.CancelRollbacker
import com.coffee.gu.cancel.CancelTxHandler
import com.coffee.gu.coupon.IssuedCouponManager
import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.PaymentMethod
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.enums.TransactionType
import com.coffee.gu.menu.LimitedMenuStockManager
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderLine
import com.coffee.gu.order.OrderManager
import com.coffee.gu.order.OrderReader
import com.coffee.gu.payment.Payment
import com.coffee.gu.payment.PaymentApprovalResult
import com.coffee.gu.payment.PaymentCompleter
import com.coffee.gu.payment.PaymentManager
import com.coffee.gu.payment.PaymentReader
import com.coffee.gu.payment.PaymentWebhookCommand
import com.coffee.gu.payment.PaymentWebhookSyncAction
import com.coffee.gu.stamp.StampHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import java.math.BigDecimal
import java.time.LocalDateTime

class PaymentWebhookSyncServiceTest {

    private val paymentReader: PaymentReader = mock()
    private val orderReader: OrderReader = mock()
    private val paymentCompleter: PaymentCompleter = mock()
    private val paymentManager: PaymentManager = mock()
    private val orderManager: OrderManager = mock()
    private val issuedCouponManager: IssuedCouponManager = mock()
    private val stampHandler: StampHandler = mock()
    private val limitedMenuStockManager: LimitedMenuStockManager = mock()
    private val cancelManager: CancelManager = mock()
    private val transactionHistoryManager: TransactionHistoryManager = mock()
    private val eventLogRepository: EventLogRepository = mock()

    private lateinit var cancelRollbacker: CancelRollbacker
    private lateinit var cancelRecorder: CancelRecorder
    private lateinit var cancelTxHandler: CancelTxHandler
    private lateinit var paymentWebhookSynchronizer: PaymentWebhookSynchronizer
    private lateinit var paymentWebhookSyncService: PaymentWebhookSyncService

    @BeforeEach
    fun setUp() {
        cancelRollbacker = CancelRollbacker(
            orderManager = orderManager,
            issuedCouponManager = issuedCouponManager,
            stampHandler = stampHandler,
            limitedMenuStockManager = limitedMenuStockManager,
        )
        cancelRecorder = CancelRecorder(
            cancelManager = cancelManager,
            transactionHistoryManager = transactionHistoryManager,
        )
        cancelTxHandler = CancelTxHandler(
            paymentManager = paymentManager,
            cancelRollbacker = cancelRollbacker,
            cancelRecorder = cancelRecorder,
            eventLogRepository = eventLogRepository,
        )
        paymentWebhookSynchronizer = PaymentWebhookSynchronizer(
            paymentManager = paymentManager,
            orderManager = orderManager,
            cancelTxHandler = cancelTxHandler,
            transactionHistoryManager = transactionHistoryManager,
            limitedMenuStockManager = limitedMenuStockManager,
        )
        paymentWebhookSyncService = PaymentWebhookSyncService(
            paymentReader = paymentReader,
            orderReader = orderReader,
            paymentCompleter = paymentCompleter,
            paymentWebhookSynchronizer = paymentWebhookSynchronizer,
        )
    }

    @Test
    @DisplayName("가상계좌 입금 완료(DONE) 웹훅 수신 시 PaymentCompleter를 통해 결제를 완료하고 이미 완료된 건은 멱등하게 무시한다")
    fun syncVirtualAccountDoneAndHandleDuplicateIdempotently() {
        val order = createSampleOrder(OrderState.CREATED)
        val pendingPayment = createSamplePayment(PaymentState.PENDING_PG)
        given(paymentReader.getByOrderKeyWithLock(order.key)).willReturn(pendingPayment)
        given(orderReader.getByOrderKey(order.key)).willReturn(order)
        given(
            paymentCompleter.complete(
                eq(order),
                eq(pendingPayment.id),
                any<PGConfirmResult>(),
            ),
        ).willAnswer {
            order.paid()
            PaymentApprovalResult.approved(order.key, "tviva-001", LocalDateTime.now())
        }

        val command = PaymentWebhookCommand(
            orderKey = order.key,
            paymentKey = "tviva-001",
            status = "DONE",
            method = PaymentMethod.VIRTUAL_ACCOUNT,
        )

        val firstResult = paymentWebhookSyncService.syncStatus(command)

        assertThat(firstResult.action).isEqualTo(PaymentWebhookSyncAction.PAID)
        assertThat(firstResult.paymentState).isEqualTo(PaymentState.SUCCESS)
        assertThat(firstResult.orderState).isEqualTo(OrderState.PAID)
        then(paymentCompleter).should().complete(eq(order), eq(pendingPayment.id), any<PGConfirmResult>())

        // 이미 SUCCESS 상태로 변경된 후 중복 DONE 웹훅 수신
        pendingPayment.success("tviva-001", PaymentMethod.VIRTUAL_ACCOUNT, null, LocalDateTime.now())
        val duplicateResult = paymentWebhookSyncService.syncStatus(command)

        assertThat(duplicateResult.action).isEqualTo(PaymentWebhookSyncAction.IGNORED_ALREADY_PROCESSED)
    }

    @Test
    @DisplayName("토스 상점관리자 직접 취소(CANCELED) 웹훅 수신 시 결제·주문을 취소하고 한정 수량 재고(DB+Redis)를 복구하며 중복 웹훅은 무시한다")
    fun syncDirectCancelFromDashboardAndRestoreLimitedStock() {
        val order = createSampleOrder(OrderState.PAID)
        val paidPayment = createSamplePayment(PaymentState.SUCCESS, issuedCouponId = 55L)
        given(paymentReader.getByOrderKeyWithLock(order.key)).willReturn(paidPayment)
        given(orderReader.getByOrderKey(order.key)).willReturn(order)
        given(orderManager.cancel(order)).willAnswer { order.canceled() }

        val canceledAt = LocalDateTime.now()
        given(cancelManager.cancel(paidPayment, "tx-cancel-777")).willReturn(
            Cancel(
                id = 1L,
                principal = order.principal,
                orderKey = order.key,
                paymentId = paidPayment.id,
                originalAmount = paidPayment.originalAmount,
                issuedCouponId = paidPayment.issuedCouponId,
                couponDiscount = paidPayment.couponDiscount,
                paidAmount = paidPayment.amount,
                canceledAmount = paidPayment.amount,
                externalCancelKey = "tx-cancel-777",
                canceledAt = canceledAt,
            ),
        )

        val command = PaymentWebhookCommand(
            orderKey = order.key,
            paymentKey = "tviva-001",
            status = "CANCELED",
            method = PaymentMethod.CARD,
            cancelTransactionKey = "tx-cancel-777",
            cancelReason = "상점관리자 직접 취소",
        )

        val result = paymentWebhookSyncService.syncStatus(command)

        assertThat(result.action).isEqualTo(PaymentWebhookSyncAction.CANCELED)
        assertThat(result.paymentState).isEqualTo(PaymentState.CANCELED)
        assertThat(result.orderState).isEqualTo(OrderState.CANCELED)

        then(limitedMenuStockManager).should().releaseIfLimited(
            orderKey = eq(order.key),
            principalKey = eq(order.principal.key),
            menuQuantities = eq(mapOf(4L to 1)),
            restoreDbSoldQuantity = eq(true),
            nowEpochMillis = any(),
        )
        then(issuedCouponManager).should().revert(order.principal, 55L)
        then(stampHandler).should().revert(order)
        then(transactionHistoryManager).should().record(
            TransactionType.CANCEL,
            order,
            paidPayment,
            "상점관리자 직접 취소",
            canceledAt,
        )
        then(eventLogRepository).should().publish(any<CancelEvent>())

        // 중복 CANCELED 웹훅 재수신 시 재고 중복 복구 없이 즉시 무시
        val duplicateResult = paymentWebhookSyncService.syncStatus(command)
        assertThat(duplicateResult.action).isEqualTo(PaymentWebhookSyncAction.IGNORED_ALREADY_PROCESSED)
    }

    @Test
    @DisplayName("가상계좌 입금 기한 만료(EXPIRED) 웹훅 수신 시 결제를 FAILED로 전환하고 선점된 Redis 한정 수량만 해제한다")
    fun syncExpiredVirtualAccountAndReleaseRedisStockOnly() {
        val order = createSampleOrder(OrderState.CREATED)
        val pendingPayment = createSamplePayment(PaymentState.PENDING_PG)
        given(paymentReader.getByOrderKeyWithLock(order.key)).willReturn(pendingPayment)
        given(orderReader.getByOrderKey(order.key)).willReturn(order)
        given(orderManager.cancel(order)).willAnswer { order.canceled() }

        val command = PaymentWebhookCommand(
            orderKey = order.key,
            paymentKey = "tviva-expired-001",
            status = "EXPIRED",
            method = PaymentMethod.VIRTUAL_ACCOUNT,
        )

        val result = paymentWebhookSyncService.syncStatus(command)

        assertThat(result.action).isEqualTo(PaymentWebhookSyncAction.FAILED)
        assertThat(result.paymentState).isEqualTo(PaymentState.FAILED)
        assertThat(result.orderState).isEqualTo(OrderState.CANCELED)
        then(limitedMenuStockManager).should().releaseIfLimited(
            orderKey = eq(order.key),
            principalKey = eq(order.principal.key),
            menuQuantities = eq(mapOf(4L to 1)),
            restoreDbSoldQuantity = eq(false),
            nowEpochMillis = any(),
        )
        then(stampHandler).should(never()).revert(any())
    }

    private fun createSampleOrder(state: OrderState): Order {
        return Order(
            key = "ORD-WEBHOOK-100",
            name = "에티오피아 게이샤 드립 1잔",
            principal = Principal.user("1"),
            storeId = 1L,
            totalPrice = BigDecimal.valueOf(7000),
            state = state,
            lines = listOf(
                OrderLine(
                    orderKey = "ORD-WEBHOOK-100",
                    menuId = 4L,
                    menuName = "에티오피아 게이샤 드립",
                    imageUrl = "https://example.com/geisha.jpg",
                    description = "스페셜티 원두",
                    quantity = 1L,
                    unitPrice = BigDecimal.valueOf(7000),
                    totalPrice = BigDecimal.valueOf(7000),
                    isStampEligible = true,
                ),
            ),
        )
    }

    private fun createSamplePayment(
        state: PaymentState,
        issuedCouponId: Long? = null,
    ): Payment {
        return Payment(
            id = 10L,
            principal = Principal.user("1"),
            orderKey = "ORD-WEBHOOK-100",
            originalAmount = BigDecimal.valueOf(7000),
            issuedCouponId = issuedCouponId,
            couponDiscount = if (issuedCouponId != null) BigDecimal.valueOf(1000) else null,
            amount = BigDecimal.valueOf(if (issuedCouponId != null) 6000 else 7000),
            state = state,
            externalPaymentKey = "tviva-001",
            method = PaymentMethod.CARD,
        )
    }
}
