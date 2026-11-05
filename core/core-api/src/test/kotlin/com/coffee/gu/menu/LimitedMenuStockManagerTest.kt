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
        given(limitedMenuFinder.getAllActiveMenuIds()).willReturn(listOf(100L, 200L))
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
        then(limitedMenuFinder).should(never()).findAllByMenuIdIn(any())
        then(reservationManager).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("한 주문에 2종류 이상의 한정판 상품이 포함되면 LIMITED_MENU_MULTIPLE_ITEMS_NOT_ALLOWED 예외가 발생한다")
    fun reserveIfLimited_multipleLimitedMenus_throwsException() {
        // given
        val limitedMenu1 = createLimitedMenu(id = 1L, menuId = 100L)
        val limitedMenu2 = createLimitedMenu(id = 2L, menuId = 200L)
        given(limitedMenuFinder.findAllByMenuIdIn(listOf(100L, 200L))).willReturn(listOf(limitedMenu1, limitedMenu2))

        // when & then
        assertThatThrownBy {
            manager.reserveIfLimited(
                orderKey = "ORD-001",
                principalKey = "USER-001",
                menuQuantities = mapOf(100L to 1, 200L to 1),
            )
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.LIMITED_MENU_MULTIPLE_ITEMS_NOT_ALLOWED)

        then(reservationManager).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("1인당 최대 구매 수량을 초과하여 요청하면 LIMITED_MENU_EXCEED_MAX_PURCHASE_QUANTITY 예외가 발생한다")
    fun reserveIfLimited_exceedsMaxPurchasePerUser_throwsException() {
        // given
        val limitedMenu = createLimitedMenu(id = 1L, menuId = 100L, maxPerUser = 1)
        given(limitedMenuFinder.findAllByMenuIdIn(listOf(100L))).willReturn(listOf(limitedMenu))

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
    @DisplayName("단일 한정 메뉴 정상 선점 성공 시 예외 없이 완료된다")
    fun reserveIfLimited_singleItem_success() {
        // given
        val limitedMenu = createLimitedMenu(id = 1L, menuId = 100L, maxPerUser = 2)
        given(limitedMenuFinder.findAllByMenuIdIn(listOf(100L))).willReturn(listOf(limitedMenu))
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
        val limitedMenu = createLimitedMenu(id = 1L, menuId = 100L)
        given(limitedMenuFinder.findAllByMenuIdIn(listOf(100L))).willReturn(listOf(limitedMenu))
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
            menuQuantities = mapOf(1L to 1),
        )

        // then
        assertThat(result).isEqualTo(ConfirmationResult.CONFIRMED)
        then(limitedMenuFinder).should(never()).findAllByMenuIdIn(any())
        then(reservationManager).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("verifyAndConfirmIfLimited는 주문 수량이 1인당 최대 구매 수량을 초과하면 LIMITED_MENU_EXCEED_MAX_PURCHASE_QUANTITY 예외가 발생한다")
    fun verifyAndConfirmIfLimited_exceedsMaxPurchasePerUser_throwsException() {
        // given
        val limitedMenu = createLimitedMenu(id = 1L, menuId = 100L, maxPerUser = 1)
        given(limitedMenuFinder.findAllByMenuIdIn(listOf(100L))).willReturn(listOf(limitedMenu))

        // when & then
        assertThatThrownBy {
            manager.verifyAndConfirmIfLimited(
                orderKey = "ORD-001",
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
        val limitedMenu = createLimitedMenu(id = 1L, menuId = 100L)
        given(limitedMenuFinder.findAllByMenuIdIn(listOf(100L))).willReturn(listOf(limitedMenu))

        // when
        manager.confirmSoldQuantityInDbIfLimited(mapOf(100L to 1))

        // then
        then(limitedMenuManager).should().increaseSoldQuantity(100L, 1)
    }

    @Test
    @DisplayName("confirmSoldQuantityInDbIfLimited는 여러 한정 메뉴가 포함되어도 각각의 DB 판매 수량을 증가시킨다")
    fun confirmSoldQuantityInDbIfLimited_multipleItems_increasesEachSoldQuantity() {
        // given
        val limitedMenu1 = createLimitedMenu(id = 1L, menuId = 100L)
        val limitedMenu2 = createLimitedMenu(id = 2L, menuId = 200L)
        given(limitedMenuFinder.findAllByMenuIdIn(listOf(100L, 200L))).willReturn(listOf(limitedMenu1, limitedMenu2))

        // when
        manager.confirmSoldQuantityInDbIfLimited(mapOf(100L to 2, 200L to 3))

        // then
        then(limitedMenuManager).should().increaseSoldQuantity(100L, 2)
        then(limitedMenuManager).should().increaseSoldQuantity(200L, 3)
    }

    @Test
    @DisplayName("verifyAndConfirmIfLimited는 여러 한정 메뉴 중 하나라도 만료(EXPIRED)되면 EXPIRED를 반환한다")
    fun verifyAndConfirmIfLimited_multipleItems_oneExpired_returnsExpired() {
        // given
        val limitedMenu1 = createLimitedMenu(id = 1L, menuId = 100L)
        val limitedMenu2 = createLimitedMenu(id = 2L, menuId = 200L)
        given(limitedMenuFinder.findAllByMenuIdIn(listOf(100L, 200L))).willReturn(listOf(limitedMenu1, limitedMenu2))
        given(reservationManager.verifyAndConfirm(eq(100L), eq("ORD-001"), any()))
            .willReturn(ConfirmationResult.CONFIRMED)
        given(reservationManager.verifyAndConfirm(eq(200L), eq("ORD-001"), any()))
            .willReturn(ConfirmationResult.EXPIRED)

        // when
        val result = manager.verifyAndConfirmIfLimited(
            orderKey = "ORD-001",
            menuQuantities = mapOf(100L to 1, 200L to 1),
        )

        // then
        assertThat(result).isEqualTo(ConfirmationResult.EXPIRED)
        then(reservationManager).should().verifyAndConfirm(eq(100L), eq("ORD-001"), any())
        then(reservationManager).should().verifyAndConfirm(eq(200L), eq("ORD-001"), any())
    }

    @Test
    @DisplayName("verifyAndConfirmIfLimited는 모든 한정 메뉴가 유효하면 CONFIRMED를 반환한다")
    fun verifyAndConfirmIfLimited_multipleItems_allConfirmed_returnsConfirmed() {
        // given
        val limitedMenu1 = createLimitedMenu(id = 1L, menuId = 100L)
        val limitedMenu2 = createLimitedMenu(id = 2L, menuId = 200L)
        given(limitedMenuFinder.findAllByMenuIdIn(listOf(100L, 200L))).willReturn(listOf(limitedMenu1, limitedMenu2))
        given(reservationManager.verifyAndConfirm(eq(100L), eq("ORD-001"), any()))
            .willReturn(ConfirmationResult.CONFIRMED)
        given(reservationManager.verifyAndConfirm(eq(200L), eq("ORD-001"), any()))
            .willReturn(ConfirmationResult.CONFIRMED)

        // when
        val result = manager.verifyAndConfirmIfLimited(
            orderKey = "ORD-001",
            menuQuantities = mapOf(100L to 1, 200L to 1),
        )

        // then
        assertThat(result).isEqualTo(ConfirmationResult.CONFIRMED)
        then(reservationManager).should().verifyAndConfirm(eq(100L), eq("ORD-001"), any())
        then(reservationManager).should().verifyAndConfirm(eq(200L), eq("ORD-001"), any())
    }
}
