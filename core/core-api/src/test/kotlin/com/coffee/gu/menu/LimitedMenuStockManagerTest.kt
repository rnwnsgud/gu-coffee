package com.coffee.gu.menu

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.MenuType
import com.coffee.gu.lock.LimitedMenuStockReservationManager
import com.coffee.gu.lock.LimitedMenuStockReservationManager.ConfirmationResult
import com.coffee.gu.lock.LimitedMenuStockReservationManager.ReservationResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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

class LimitedMenuStockManagerTest {

    private val limitedMenuFinder: LimitedMenuFinder = mock()
    private val limitedMenuManager: LimitedMenuManager = mock()
    private val reservationManager: LimitedMenuStockReservationManager = mock()

    private val manager = LimitedMenuStockManager(
        limitedMenuFinder = limitedMenuFinder,
        limitedMenuManager = limitedMenuManager,
        reservationManager = reservationManager,
    )

    @BeforeEach
    fun setUp() {
        given(limitedMenuFinder.getAllActivePolicies()).willReturn(
            listOf(
                LimitedMenuPolicy(100L, 1),
                LimitedMenuPolicy(200L, 1),
            )
        )
    }

    private fun createLimitedMenu(id: Long, menuId: Long, totalQty: Int = 10, maxPerUser: Int = 1): LimitedMenu {
        val menu = Menu(
            id = menuId,
            name = "한정판 상품 $menuId",
            type = MenuType.MD,
            price = Price(salesPrice = BigDecimal("10000"), costPrice = BigDecimal("5000")),
        )
        return LimitedMenu(
            id = id,
            menu = menu,
            totalQuantity = totalQty,
            soldQuantity = 0,
            maxPurchasePerUser = maxPerUser,
        )
    }

    @Test
    @DisplayName("주문서에 한정 메뉴가 없으면 예약하지 않고 즉시 반환한다 (DB 조회 0회 검증)")
    fun reserveIfLimited_emptyLimitedMenus_doesNothing() {
        // given (메뉴 1L은 활성 한정 메뉴 목록에 없음)

        // when
        manager.reserveIfLimited(
            orderKey = "ORD-001",
            principalKey = "USER-001",
            menuQuantities = mapOf(1L to 2),
        )

        // then
        then(limitedMenuFinder).should(never()).getByMenuId(any())
        then(reservationManager).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("한 주문에 서로 다른 종류의 한정 메뉴를 각 1개씩 포함하면 모두 정상 선점된다 (DB 조회 0회)")
    fun reserveIfLimited_multipleLimitedMenus_success() {
        // given
        given(
            reservationManager.reserve(
                menuId = any(),
                orderKey = any(),
                principalKey = any(),
                quantity = any(),
                ttlMillis = any(),
                nowEpochMillis = any(),
                stockSupplier = any(),
            )
        ).willReturn(ReservationResult.RESERVED)

        // when
        manager.reserveIfLimited(
            orderKey = "ORD-001",
            principalKey = "USER-001",
            menuQuantities = mapOf(100L to 1, 200L to 1),
        )

        // then
        then(limitedMenuFinder).should(never()).getByMenuId(any())
        then(reservationManager).should().reserve(
            menuId = eq(100L),
            orderKey = eq("ORD-001"),
            principalKey = eq("USER-001"),
            quantity = eq(1),
            ttlMillis = any(),
            nowEpochMillis = any(),
            stockSupplier = any(),
        )
        then(reservationManager).should().reserve(
            menuId = eq(200L),
            orderKey = eq("ORD-001"),
            principalKey = eq("USER-001"),
            quantity = eq(1),
            ttlMillis = any(),
            nowEpochMillis = any(),
            stockSupplier = any(),
        )
    }

    @Test
    @DisplayName("복수 종류 한정 메뉴 선점 중 두 번째 품목이 품절되면 이미 선점된 첫 번째 품목을 즉시 보상 해제(release)한다")
    fun reserveIfLimited_multipleLimitedMenus_secondSoldOut_releasesFirst() {
        // given
        given(
            reservationManager.reserve(
                menuId = eq(100L),
                orderKey = any(),
                principalKey = any(),
                quantity = any(),
                ttlMillis = any(),
                nowEpochMillis = any(),
                stockSupplier = any(),
            )
        ).willReturn(ReservationResult.RESERVED)
        given(
            reservationManager.reserve(
                menuId = eq(200L),
                orderKey = any(),
                principalKey = any(),
                quantity = any(),
                ttlMillis = any(),
                nowEpochMillis = any(),
                stockSupplier = any(),
            )
        ).willReturn(ReservationResult.SOLD_OUT)

        // when & then
        assertThatThrownBy {
            manager.reserveIfLimited(
                orderKey = "ORD-001",
                principalKey = "USER-001",
                menuQuantities = mapOf(100L to 1, 200L to 1),
            )
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.LIMITED_MENU_SOLD_OUT)

        then(reservationManager).should().release(
            menuId = eq(100L),
            orderKey = eq("ORD-001"),
            principalKey = eq("USER-001"),
            quantity = eq(1),
            nowEpochMillis = any(),
        )
    }

    @Test
    @DisplayName("1인당 최대 구매 수량을 초과하여 요청하면 LIMITED_MENU_EXCEED_MAX_PURCHASE_QUANTITY 예외가 발생한다")
    fun reserveIfLimited_exceedsMaxPurchasePerUser_throwsException() {
        // given (100L의 maxPurchasePerUser는 1)

        // when & then
        assertThatThrownBy {
            manager.reserveIfLimited(
                orderKey = "ORD-001",
                principalKey = "USER-001",
                menuQuantities = mapOf(100L to 2),
            )
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.LIMITED_MENU_EXCEED_MAX_PURCHASE_QUANTITY)

        then(reservationManager).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("단일 한정 메뉴 정상 선점 성공 시 예외 없이 완료된다 (DB 조회 0회)")
    fun reserveIfLimited_singleItem_success() {
        // given
        given(
            reservationManager.reserve(
                menuId = any(),
                orderKey = any(),
                principalKey = any(),
                quantity = any(),
                ttlMillis = any(),
                nowEpochMillis = any(),
                stockSupplier = any(),
            )
        ).willReturn(ReservationResult.RESERVED)

        // when
        manager.reserveIfLimited(
            orderKey = "ORD-001",
            principalKey = "USER-001",
            menuQuantities = mapOf(100L to 1),
        )

        // then
        then(limitedMenuFinder).should(never()).getByMenuId(any())
        then(reservationManager).should().reserve(
            menuId = any(),
            orderKey = any(),
            principalKey = any(),
            quantity = any(),
            ttlMillis = any(),
            nowEpochMillis = any(),
            stockSupplier = any(),
        )
    }

    @Test
    @DisplayName("재고 부족으로 SOLD_OUT 결과 반환 시 LIMITED_MENU_SOLD_OUT 예외가 발생한다")
    fun reserveIfLimited_soldOut_throwsException() {
        // given
        given(
            reservationManager.reserve(
                menuId = any(),
                orderKey = any(),
                principalKey = any(),
                quantity = any(),
                ttlMillis = any(),
                nowEpochMillis = any(),
                stockSupplier = any(),
            )
        ).willReturn(ReservationResult.SOLD_OUT)

        // when & then
        assertThatThrownBy {
            manager.reserveIfLimited(
                orderKey = "ORD-001",
                principalKey = "USER-001",
                menuQuantities = mapOf(100L to 1),
            )
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.LIMITED_MENU_SOLD_OUT)
    }

    @Test
    @DisplayName("verifyAndConfirmIfLimited는 한정 메뉴가 없으면 즉시 CONFIRMED를 반환한다")
    fun verifyAndConfirmIfLimited_empty_returnsConfirmed() {
        // given (1L은 한정 메뉴가 아님)

        // when
        val result = manager.verifyAndConfirmIfLimited(
            orderKey = "ORD-001",
            principalKey = "USER-001",
            menuQuantities = mapOf(1L to 1),
        )

        // then
        assertThat(result).isEqualTo(ConfirmationResult.CONFIRMED)
        then(limitedMenuFinder).should(never()).getByMenuId(any())
        then(reservationManager).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("verifyAndConfirmIfLimited는 주문 수량이 1인당 최대 구매 수량을 초과하면 LIMITED_MENU_EXCEED_MAX_PURCHASE_QUANTITY 예외가 발생한다")
    fun verifyAndConfirmIfLimited_exceedsMaxPurchasePerUser_throwsException() {
        // given (100L의 maxPurchasePerUser는 1)

        // when & then
        assertThatThrownBy {
            manager.verifyAndConfirmIfLimited(
                orderKey = "ORD-001",
                principalKey = "USER-001",
                menuQuantities = mapOf(100L to 2),
            )
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.LIMITED_MENU_EXCEED_MAX_PURCHASE_QUANTITY)

        then(reservationManager).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("confirmSoldQuantityInDbIfLimited는 단일 한정 메뉴의 DB 판매 수량을 증가시킨다")
    fun confirmSoldQuantityInDbIfLimited_singleItem_increasesSoldQuantity() {
        // given

        // when
        manager.confirmSoldQuantityInDbIfLimited(mapOf(100L to 1))

        // then
        then(limitedMenuManager).should().increaseSoldQuantity(100L, 1)
    }

    @Test
    @DisplayName("confirmSoldQuantityInDbIfLimited는 여러 한정 메뉴가 포함되어도 각각의 DB 판매 수량을 증가시킨다")
    fun confirmSoldQuantityInDbIfLimited_multipleItems_increasesEachSoldQuantity() {
        // given

        // when
        manager.confirmSoldQuantityInDbIfLimited(mapOf(100L to 2, 200L to 3))

        // then
        then(limitedMenuManager).should().increaseSoldQuantity(100L, 2)
        then(limitedMenuManager).should().increaseSoldQuantity(200L, 3)
    }

    @Test
    @DisplayName("verifyAndConfirmIfLimited는 여러 한정 메뉴 중 하나라도 만료(EXPIRED)되면 이미 확정된 품목을 롤백(release)하고 EXPIRED를 반환한다")
    fun verifyAndConfirmIfLimited_multipleItems_oneExpired_returnsExpired() {
        // given
        given(reservationManager.verifyAndConfirm(eq(100L), eq("ORD-001"), any()))
            .willReturn(ConfirmationResult.CONFIRMED)
        given(reservationManager.verifyAndConfirm(eq(200L), eq("ORD-001"), any()))
            .willReturn(ConfirmationResult.EXPIRED)

        // when
        val result = manager.verifyAndConfirmIfLimited(
            orderKey = "ORD-001",
            principalKey = "USER-001",
            menuQuantities = mapOf(100L to 1, 200L to 1),
        )

        // then
        assertThat(result).isEqualTo(ConfirmationResult.EXPIRED)
        then(reservationManager).should().verifyAndConfirm(eq(100L), eq("ORD-001"), any())
        then(reservationManager).should().verifyAndConfirm(eq(200L), eq("ORD-001"), any())
        then(reservationManager).should().release(
            menuId = eq(100L),
            orderKey = eq("ORD-001"),
            principalKey = eq("USER-001"),
            quantity = eq(1),
            nowEpochMillis = any(),
        )
    }

    @Test
    @DisplayName("verifyAndConfirmIfLimited는 모든 한정 메뉴가 유효하면 CONFIRMED를 반환한다")
    fun verifyAndConfirmIfLimited_multipleItems_allConfirmed_returnsConfirmed() {
        // given
        given(reservationManager.verifyAndConfirm(eq(100L), eq("ORD-001"), any()))
            .willReturn(ConfirmationResult.CONFIRMED)
        given(reservationManager.verifyAndConfirm(eq(200L), eq("ORD-001"), any()))
            .willReturn(ConfirmationResult.CONFIRMED)

        // when
        val result = manager.verifyAndConfirmIfLimited(
            orderKey = "ORD-001",
            principalKey = "USER-001",
            menuQuantities = mapOf(100L to 1, 200L to 1),
        )

        // then
        assertThat(result).isEqualTo(ConfirmationResult.CONFIRMED)
        then(reservationManager).should().verifyAndConfirm(eq(100L), eq("ORD-001"), any())
        then(reservationManager).should().verifyAndConfirm(eq(200L), eq("ORD-001"), any())
        then(reservationManager).should(never()).release(any(), any(), any(), any(), any())
    }
}
