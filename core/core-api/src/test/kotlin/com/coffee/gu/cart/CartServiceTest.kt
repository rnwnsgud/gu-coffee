package com.coffee.gu.cart

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.MenuType
import com.coffee.gu.menu.Menu
import com.coffee.gu.menu.MenuFinder
import com.coffee.gu.menu.Price
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class CartServiceTest {

    @InjectMocks
    private lateinit var cartService: CartService

    @Mock
    private lateinit var cartReader: CartReader

    @Mock
    private lateinit var cartItemManager: CartItemManager

    @Mock
    private lateinit var menuFinder: MenuFinder

    private fun createPrincipal(): Principal {
        return Principal.user("1L")
    }

    @Nested
    @DisplayName("getCart 메서드는")
    inner class Describe_getCart {

        @Test
        @DisplayName("장바구니에 아이템이 있으면 아이템 목록이 포함된 Cart를 반환한다")
        fun it_returns_cart_with_items() {
            // given
            val principal = createPrincipal()
            val menu = Menu(100L, "Coffee", MenuType.DRINK, Price(BigDecimal.ZERO, BigDecimal.ZERO), null, null, null)
            val cartItem = CartItem(1L, menu, 2L, false)

            given(cartReader.findByPrincipal(principal)).willReturn(listOf(cartItem))

            // when
            val cart = cartService.getCart(principal)

            // then
            assertThat(cart.items).hasSize(1)
            assertThat(cart.items[0].quantity).isEqualTo(2L)
            assertThat(cart.items[0].menu.name).isEqualTo("Coffee")
        }

        @Test
        @DisplayName("장바구니가 비어있으면 빈 목록이 포함된 Cart를 반환한다")
        fun it_returns_empty_cart_when_cart_is_empty() {
            // given
            val principal = createPrincipal()
            given(cartReader.findByPrincipal(principal)).willReturn(emptyList())

            // when
            val cart = cartService.getCart(principal)

            // then
            assertThat(cart.items).isEmpty()
        }
    }

    @Nested
    @DisplayName("addCartItem 메서드는")
    inner class Describe_addCartItem {

        @Test
        @DisplayName("새로운 아이템을 추가하면 매니저에게 위임하고 아이디를 반환한다")
        fun it_delegates_to_manager_and_returns_id() {
            // given
            val principal = createPrincipal()
            val addCartItem = AddCartItem(100L, 1L)
            given(cartReader.findByPrincipalAndMenuId(principal, 100L)).willReturn(null)
            given(cartItemManager.addCartItem(eq(principal), eq(addCartItem), anyOrNull())).willReturn(1L)

            // when
            val resultId = cartService.addCartItem(principal, addCartItem)

            // then
            assertThat(resultId).isEqualTo(1L)
            verify(cartItemManager).addCartItem(eq(principal), eq(addCartItem), anyOrNull())
        }
    }

    @Nested
    @DisplayName("modifyCartItem 메서드는")
    inner class Describe_modifyCartItem {

        @Test
        @DisplayName("아이템 수량을 수정하면 아이디를 반환한다")
        fun it_modifies_quantity_and_returns_id() {
            // given
            val principal = createPrincipal()
            val modifyCartItem = ModifyCartItem(1L, 5L)
            val menu = Menu(1L, "Coffee", MenuType.DRINK, Price(BigDecimal.ZERO, BigDecimal.ZERO), null, null, null)
            val cartItem = CartItem(1L, menu, 1L, false)

            given(cartReader.getByPrincipalAndId(principal, 1L)).willReturn(cartItem)

            // when
            val resultId = cartService.modifyCartItem(principal, modifyCartItem)

            // then
            assertThat(resultId).isEqualTo(1L)
            verify(cartItemManager).modifyCartItem(eq(cartItem), eq(modifyCartItem.quantity), eq(principal))
        }

        @Test
        @DisplayName("존재하지 않는 아이템을 수정하려 하면 예외가 발생한다")
        fun it_throws_exception_when_item_not_found() {
            // given
            val principal = createPrincipal()
            val modifyCartItem = ModifyCartItem(999L, 5L)
            given(cartReader.getByPrincipalAndId(principal, 999L))
                .willThrow(CoreException(ErrorType.NOT_FOUND_DATA, null))

            // when & then
            assertThatThrownBy { cartService.modifyCartItem(principal, modifyCartItem) }
                .isInstanceOf(CoreException::class.java)
        }
    }
}
