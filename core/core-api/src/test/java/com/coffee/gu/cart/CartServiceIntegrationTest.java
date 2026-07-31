package com.coffee.gu.cart;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.MenuType;
import com.coffee.gu.menu.MenuEntity;
import com.coffee.gu.menu.MenuJpaRepository;
import com.coffee.gu.TestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestApplication.class)
@Transactional
@ActiveProfiles("local")
class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemJpaRepository cartItemRepository;

    @Autowired
    private MenuJpaRepository menuRepository;

    private Principal principal;
    private Long menuId;

    @BeforeEach
    void setUp() throws Exception {
        principal = Principal.user("1L");

        MenuEntity menuEntity = new MenuEntity();
        ReflectionTestUtils.setField(menuEntity, "name", "Test Menu");
        ReflectionTestUtils.setField(menuEntity, "type", MenuType.DRINK);
        ReflectionTestUtils.setField(menuEntity, "costPrice", BigDecimal.valueOf(1000));
        ReflectionTestUtils.setField(menuEntity, "salesPrice", BigDecimal.valueOf(2000));
        ReflectionTestUtils.setField(menuEntity, "description", "Description");
        menuEntity.active();

        menuRepository.saveAndFlush(menuEntity);
        menuId = menuEntity.getId();
    }

    @Test
    @DisplayName("장바구니에 상품을 추가하고 조회할 수 있다")
    void addAndGetCart() {
        // given
        AddCartItem addCartItem = new AddCartItem(menuId, 2L);

        // when
        cartService.addCartItem(principal, addCartItem);
        Cart cart = cartService.getCart(principal);

        // then
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getMenu().getId()).isEqualTo(menuId);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2L);
    }

    @Test
    @DisplayName("장바구니 아이템의 수량을 수정할 수 있다")
    void modifyCartItem() {
        // given
        AddCartItem addCartItem = new AddCartItem(menuId, 1L);
        Long cartItemId = cartService.addCartItem(principal, addCartItem);

        // when
        ModifyCartItem modifyCartItem = new ModifyCartItem(cartItemId, 5L);
        cartService.modifyCartItem(principal, modifyCartItem);
        
        // then
        CartItemEntity updated = cartItemRepository.findById(cartItemId).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(5L);
    }

    @Test
    @DisplayName("이미 존재하는 메뉴를 추가하면 수량이 합산된다")
    void addExistingMenu() {
        // given
        cartService.addCartItem(principal, new AddCartItem(menuId, 1L));

        // when
        cartService.addCartItem(principal, new AddCartItem(menuId, 2L));

        // then
        Cart cart = cartService.getCart(principal);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3L);
    }
}
