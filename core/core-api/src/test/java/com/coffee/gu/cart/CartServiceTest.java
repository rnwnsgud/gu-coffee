package com.coffee.gu.cart;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.Principal;
import com.coffee.gu.menu.Menu;
import com.coffee.gu.menu.MenuFinder;
import com.coffee.gu.enums.MenuType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @InjectMocks
    private CartService cartService;

    @Mock
    private CartReader cartReader;

    @Mock
    private CartItemManager cartItemManager;

    @Mock
    private MenuFinder menuFinder;

    private Principal createPrincipal() {
        return Principal.user("1L");
    }

    @Nested
    @DisplayName("getCart 메서드는")
    class Describe_getCart {

        @Test
        @DisplayName("장바구니에 아이템이 있으면 아이템 목록이 포함된 Cart를 반환한다")
        void it_returns_cart_with_items() {
            // given
            Principal principal = createPrincipal();
            Menu menu = new Menu(100L, "Coffee", MenuType.DRINK,null, null, null, null);
            CartItem cartItem = new CartItem(1L, menu, 2L, false);
            
            given(cartReader.findByPrincipal(principal)).willReturn(List.of(cartItem));
            
            // when
            Cart cart = cartService.getCart(principal);

            // then
            assertThat(cart.getItems()).hasSize(1);
            assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2L);
            assertThat(cart.getItems().get(0).getMenu().getName()).isEqualTo("Coffee");
        }

        @Test
        @DisplayName("장바구니가 비어있으면 빈 목록이 포함된 Cart를 반환한다")
        void it_returns_empty_cart_when_cart_is_empty() {
            // given
            Principal principal = createPrincipal();
            given(cartReader.findByPrincipal(principal)).willReturn(Collections.emptyList());

            // when
            Cart cart = cartService.getCart(principal);

            // then
            assertThat(cart.getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("addCartItem 메서드는")
    class Describe_addCartItem {

        @Test
        @DisplayName("새로운 아이템을 추가하면 매니저에게 위임하고 아이디를 반환한다")
        void it_delegates_to_manager_and_returns_id() {
            // given
            Principal principal = createPrincipal();
            AddCartItem addCartItem = new AddCartItem(100L, 1L);
            given(cartReader.findByPrincipalAndMenuId(principal, 100L)).willReturn(Optional.empty());
            given(cartItemManager.addCartItem(eq(principal), eq(addCartItem), any())).willReturn(1L);

            // when
            Long resultId = cartService.addCartItem(principal, addCartItem);

            // then
            assertThat(resultId).isEqualTo(1L);
            verify(cartItemManager).addCartItem(eq(principal), eq(addCartItem), any());
        }
    }

    @Nested
    @DisplayName("modifyCartItem 메서드는")
    class Describe_modifyCartItem {

        @Test
        @DisplayName("아이템 수량을 수정하면 아이디를 반환한다")
        void it_modifies_quantity_and_returns_id() {
            // given
            Principal principal = createPrincipal();
            ModifyCartItem modifyCartItem = new ModifyCartItem(1L, 5L);
            CartItem cartItem = new CartItem(1L, null, 1L, false);

            given(cartReader.getByPrincipalAndId(principal, 1L)).willReturn(cartItem);

            // when
            Long resultId = cartService.modifyCartItem(principal, modifyCartItem);

            // then
            assertThat(resultId).isEqualTo(1L);
            verify(cartItemManager).modifyCartItem(eq(cartItem), eq(modifyCartItem.quantity()), eq(principal));
        }

        @Test
        @DisplayName("존재하지 않는 아이템을 수정하려 하면 예외가 발생한다")
        void it_throws_exception_when_item_not_found() {
            // given
            Principal principal = createPrincipal();
            ModifyCartItem modifyCartItem = new ModifyCartItem(999L, 5L);
            given(cartReader.getByPrincipalAndId(principal, 999L))
                    .willThrow(new CoreException(ErrorType.NOT_FOUND_DATA, null));

            // when & then
            assertThatThrownBy(() -> cartService.modifyCartItem(principal, modifyCartItem))
                    .isInstanceOf(CoreException.class);
        }
    }
}
