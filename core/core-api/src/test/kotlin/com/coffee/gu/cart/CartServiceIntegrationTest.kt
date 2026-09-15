package com.coffee.gu.cart

import com.coffee.gu.Principal
import com.coffee.gu.TestApplication
import com.coffee.gu.enums.MenuType
import com.coffee.gu.menu.MenuEntity
import com.coffee.gu.menu.MenuJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@SpringBootTest(classes = [TestApplication::class])
@Transactional
@ActiveProfiles("local")
class CartServiceIntegrationTest {

    @Autowired
    private lateinit var cartService: CartService

    @Autowired
    private lateinit var cartItemRepository: CartItemJpaRepository

    @Autowired
    private lateinit var menuRepository: MenuJpaRepository

    private lateinit var principal: Principal
    private var menuId: Long = 0L

    @BeforeEach
    fun setUp() {
        principal = Principal.user("1L")

        val menuEntity = MenuEntity(
            name = "Test Menu",
            type = MenuType.DRINK,
            costPrice = BigDecimal.valueOf(1000),
            salesPrice = BigDecimal.valueOf(2000),
            description = "Description"
        )
        menuEntity.active()

        menuRepository.saveAndFlush(menuEntity)
        menuId = menuEntity.id
    }

    @Test
    @DisplayName("장바구니에 상품을 추가하고 조회할 수 있다")
    fun addAndGetCart() {
        // given
        val addCartItem = AddCartItem(menuId, 2L)

        // when
        cartService.addCartItem(principal, addCartItem)
        val cart = cartService.getCart(principal)

        // then
        assertThat(cart.items).hasSize(1)
        assertThat(cart.items[0].menu.id).isEqualTo(menuId)
        assertThat(cart.items[0].quantity).isEqualTo(2L)
    }

    @Test
    @DisplayName("장바구니 아이템의 수량을 수정할 수 있다")
    fun modifyCartItem() {
        // given
        val addCartItem = AddCartItem(menuId, 1L)
        val cartItemId = cartService.addCartItem(principal, addCartItem)

        // when
        val modifyCartItem = ModifyCartItem(cartItemId, 5L)
        cartService.modifyCartItem(principal, modifyCartItem)

        // then
        val updated = cartItemRepository.findById(cartItemId).orElseThrow()
        assertThat(updated.quantity).isEqualTo(5L)
    }

    @Test
    @DisplayName("이미 존재하는 메뉴를 추가하면 수량이 합산된다")
    fun addExistingMenu() {
        // given
        cartService.addCartItem(principal, AddCartItem(menuId, 1L))

        // when
        cartService.addCartItem(principal, AddCartItem(menuId, 2L))

        // then
        val cart = cartService.getCart(principal)
        assertThat(cart.items).hasSize(1)
        assertThat(cart.items[0].quantity).isEqualTo(3L)
    }
}
