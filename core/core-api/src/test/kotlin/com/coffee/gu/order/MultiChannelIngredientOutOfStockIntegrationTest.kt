package com.coffee.gu.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.TestApplication
import com.coffee.gu.api.controller.v1.ExternalOrderController
import com.coffee.gu.api.controller.v1.OrderController
import com.coffee.gu.api.controller.v1.PartnerIngredientController
import com.coffee.gu.api.controller.v1.request.CreateExternalOrderLineRequest
import com.coffee.gu.api.controller.v1.request.CreateExternalOrderRequest
import com.coffee.gu.api.controller.v1.request.CreateOrderFromCartRequest
import com.coffee.gu.api.controller.v1.request.CreateOrderRequest
import com.coffee.gu.api.controller.v1.request.UpdateIngredientAvailabilityRequest
import com.coffee.gu.api.controller.v1.request.UpdateIngredientStockRequest
import com.coffee.gu.cart.AddCartItem
import com.coffee.gu.cart.CartService
import com.coffee.gu.enums.MenuType
import com.coffee.gu.enums.OrderChannel
import com.coffee.gu.enums.StoreStatus
import com.coffee.gu.menu.Ingredient
import com.coffee.gu.menu.IngredientRepository
import com.coffee.gu.menu.MenuEntity
import com.coffee.gu.menu.MenuJpaRepository
import com.coffee.gu.menu.MenuRecipe
import com.coffee.gu.menu.MenuRecipeRepository
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

@SpringBootTest(classes = [TestApplication::class])
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MultiChannelIngredientOutOfStockIntegrationTest {

    @Autowired
    private lateinit var partnerIngredientController: PartnerIngredientController

    @Autowired
    private lateinit var orderController: OrderController

    @Autowired
    private lateinit var externalOrderController: ExternalOrderController

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var cartService: CartService

    @Autowired
    private lateinit var storeRepository: StoreRepository

    @Autowired
    private lateinit var menuJpaRepository: MenuJpaRepository

    @Autowired
    private lateinit var ingredientRepository: IngredientRepository

    @Autowired
    private lateinit var menuRecipeRepository: MenuRecipeRepository

    private var gangnamStoreId: Long = 0L
    private var hongdaeStoreId: Long = 0L

    private var americanoMenuId: Long = 0L
    private var latteMenuId: Long = 0L
    private var morningSetMenuId: Long = 0L

    private val beanIngredientId = 100L
    private val milkIngredientId = 200L
    private val bagelIngredientId = 300L

    @BeforeEach
    fun setUp() {
        val gangnamStore = storeRepository.save(createStore("구커피 강남점", "GN01"))
        val hongdaeStore = storeRepository.save(createStore("구커피 홍대점", "HD01"))
        gangnamStoreId = gangnamStore.id
        hongdaeStoreId = hongdaeStore.id

        americanoMenuId = saveMenu("아메리카노", MenuType.DRINK, BigDecimal("1000.00"), BigDecimal("4500.00"))
        latteMenuId = saveMenu("카페라떼", MenuType.DRINK, BigDecimal("1500.00"), BigDecimal("5500.00"))
        morningSetMenuId = saveMenu("모닝세트(라떼+베이글)", MenuType.DESSERT, BigDecimal("3000.00"), BigDecimal("8500.00"))

        // 레시피(BOM) 구성:
        // - 아메리카노: 원두(100) 1개
        // - 카페라떼: 원두(100) 1개 + 우유(200) 1개
        // - 모닝세트: 원두(100) 1개 + 우유(200) 1개 + 베이글(300) 1개
        menuRecipeRepository.saveAll(
            listOf(
                MenuRecipe(menuId = americanoMenuId, ingredientId = beanIngredientId, requiredQuantity = 1),
                MenuRecipe(menuId = latteMenuId, ingredientId = beanIngredientId, requiredQuantity = 1),
                MenuRecipe(menuId = latteMenuId, ingredientId = milkIngredientId, requiredQuantity = 1),
                MenuRecipe(menuId = morningSetMenuId, ingredientId = beanIngredientId, requiredQuantity = 1),
                MenuRecipe(menuId = morningSetMenuId, ingredientId = milkIngredientId, requiredQuantity = 1),
                MenuRecipe(menuId = morningSetMenuId, ingredientId = bagelIngredientId, requiredQuantity = 1),
            ),
        )

        // 매장별 초기 원자재 재고 세팅 (강남점, 홍대점 모두 충분한 재고로 시작)
        ingredientRepository.saveAll(
            listOf(
                Ingredient(storeId = gangnamStoreId, ingredientId = beanIngredientId, name = "원두", stockQuantity = 100, isAvailable = true),
                Ingredient(storeId = gangnamStoreId, ingredientId = milkIngredientId, name = "우유", stockQuantity = 50, isAvailable = true),
                Ingredient(storeId = gangnamStoreId, ingredientId = bagelIngredientId, name = "베이글", stockQuantity = 20, isAvailable = true),
                Ingredient(storeId = hongdaeStoreId, ingredientId = beanIngredientId, name = "원두", stockQuantity = 100, isAvailable = true),
                Ingredient(storeId = hongdaeStoreId, ingredientId = milkIngredientId, name = "우유", stockQuantity = 50, isAvailable = true),
                Ingredient(storeId = hongdaeStoreId, ingredientId = bagelIngredientId, name = "베이글", stockQuantity = 20, isAvailable = true),
            ),
        )
    }

    @Test
    @DisplayName("강남점 점주가 우유를 품절 처리하면 앱, 장바구니, 외부 배달앱, 키오스크로 유입되는 강남점의 라떼(단품) 및 모닝세트 주문이 모두 차단되고, 아메리카노 및 홍대점 주문은 정상 통과한다")
    fun blockAllOrderChannelsForSingleAndSetMenuWhenStoreOwnerMarksIngredientSoldOut() {
        val customer = Principal.user("CUSTOMER-1")

        // 1. 품절 전에 고객이 미리 '모닝세트(라떼+베이글)'를 장바구니에 담아둠
        val cartItemId = cartService.addCartItem(customer, AddCartItem(menuId = morningSetMenuId, quantity = 1L))

        // 2. 강남점 사장님이 파트너 API를 통해 강남점의 '우유(200)'를 품절(isAvailable = false) 처리
        val partnerResponse = partnerIngredientController.updateAvailability(
            storeId = gangnamStoreId,
            ingredientId = milkIngredientId,
            request = UpdateIngredientAvailabilityRequest(isAvailable = false),
        )
        assertThat(partnerResponse.data?.isAvailable).isFalse()

        // [차단 검증 1] 자체 앱 즉시 주문 (OrderChannel.APP) - 단품 카페라떼 주문 시 차단
        assertThatThrownBy {
            orderController.createOrder(
                principal = customer,
                request = CreateOrderRequest(menuId = latteMenuId, quantity = 1L, storeId = gangnamStoreId),
            )
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.MENU_INGREDIENT_OUT_OF_STOCK)

        // [차단 검증 2] 자체 앱 즉시 주문 (OrderChannel.APP) - 우유가 포함된 모닝세트 주문 시 차단
        assertThatThrownBy {
            orderController.createOrder(
                principal = customer,
                request = CreateOrderRequest(menuId = morningSetMenuId, quantity = 1L, storeId = gangnamStoreId),
            )
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.MENU_INGREDIENT_OUT_OF_STOCK)

        // [차단 검증 3] 품절 전에 미리 담아두었던 장바구니 주문 (OrderChannel.CART) - 모닝세트 결제 진행 시 차단
        assertThatThrownBy {
            orderController.createOrderFromCart(
                principal = customer,
                request = CreateOrderFromCartRequest(storeId = gangnamStoreId, cartItemIds = setOf(cartItemId)),
            )
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.MENU_INGREDIENT_OUT_OF_STOCK)

        // [차단 검증 4] 외부 배달 플랫폼 연동 주문 (OrderChannel.DELIVERY_PARTNER) - 외부 앱에 품절 동기화가 늦어 모닝세트 주문이 인입되어도 차단
        assertThatThrownBy {
            externalOrderController.createExternalOrder(
                CreateExternalOrderRequest(
                    storeId = gangnamStoreId,
                    channel = OrderChannel.DELIVERY_PARTNER,
                    externalCustomerKey = "BAEMIN-USER-99",
                    lines = listOf(CreateExternalOrderLineRequest(menuId = morningSetMenuId, quantity = 1L)),
                ),
            )
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.MENU_INGREDIENT_OUT_OF_STOCK)

        // [차단 검증 5] 매장 키오스크 주문 (OrderChannel.KIOSK) - 라떼 + 아메리카노 복합 주문 시 우유 품절로 전체 차단
        assertThatThrownBy {
            externalOrderController.createExternalOrder(
                CreateExternalOrderRequest(
                    storeId = gangnamStoreId,
                    channel = OrderChannel.KIOSK,
                    externalCustomerKey = "KIOSK-01",
                    lines = listOf(
                        CreateExternalOrderLineRequest(menuId = americanoMenuId, quantity = 1L),
                        CreateExternalOrderLineRequest(menuId = latteMenuId, quantity = 1L),
                    ),
                ),
            )
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.MENU_INGREDIENT_OUT_OF_STOCK)

        // [정상 통과 검증 1] 강남점이라도 우유가 들어가지 않는 '아메리카노'는 정상 주문 생성
        val gangnamAmericanoOrderRes = orderController.createOrder(
            principal = customer,
            request = CreateOrderRequest(menuId = americanoMenuId, quantity = 1L, storeId = gangnamStoreId),
        )
        val gangnamAmericanoOrder = orderService.getOrder(gangnamAmericanoOrderRes.data!!.orderKey)
        assertThat(gangnamAmericanoOrder.channel).isEqualTo(OrderChannel.APP)
        assertThat(gangnamAmericanoOrder.storeId).isEqualTo(gangnamStoreId)

        // [정상 통과 검증 2] 타 매장(홍대점)은 우유가 가용 상태이므로 외부 배달앱 및 장바구니를 통한 모닝세트 주문 모두 정상 통과
        val hongdaeDeliveryOrderRes = externalOrderController.createExternalOrder(
            CreateExternalOrderRequest(
                storeId = hongdaeStoreId,
                channel = OrderChannel.DELIVERY_PARTNER,
                externalCustomerKey = "COUPANG-EATS-77",
                lines = listOf(CreateExternalOrderLineRequest(menuId = morningSetMenuId, quantity = 1L)),
            ),
        )
        val hongdaeDeliveryOrder = orderService.getOrder(hongdaeDeliveryOrderRes.data!!.orderKey)
        assertThat(hongdaeDeliveryOrder.channel).isEqualTo(OrderChannel.DELIVERY_PARTNER)
        assertThat(hongdaeDeliveryOrder.storeId).isEqualTo(hongdaeStoreId)

        val hongdaeCartOrderRes = orderController.createOrderFromCart(
            principal = customer,
            request = CreateOrderFromCartRequest(storeId = hongdaeStoreId, cartItemIds = setOf(cartItemId)),
        )
        val hongdaeCartOrder = orderService.getOrder(hongdaeCartOrderRes.data!!.orderKey)
        assertThat(hongdaeCartOrder.channel).isEqualTo(OrderChannel.CART)
        assertThat(hongdaeCartOrder.storeId).isEqualTo(hongdaeStoreId)
    }

    @Test
    @DisplayName("점주가 원자재 재고 수량을 1개로 줄이면 단품 1개 + 세트 1개 복합 주문(총 2개 소요)은 차단되고, 재고를 보충하면 다시 통과한다")
    fun blockAndRecoverWhenStoreOwnerUpdatesIngredientStockQuantity() {
        // 1. 강남점 사장님이 베이글(300)은 충분하지만 우유(200) 재고를 1개로 수정
        partnerIngredientController.updateStockQuantity(
            storeId = gangnamStoreId,
            ingredientId = milkIngredientId,
            request = UpdateIngredientStockRequest(stockQuantity = 1),
        )

        // 2. 외부 배달앱을 통해 카페라떼 1잔(우유 1) + 모닝세트 1개(우유 1) = 총 우유 2개 소요 주문 인입 -> 재고 1개 초과로 차단
        assertThatThrownBy {
            externalOrderController.createExternalOrder(
                CreateExternalOrderRequest(
                    storeId = gangnamStoreId,
                    channel = OrderChannel.DELIVERY_PARTNER,
                    externalCustomerKey = "DELIVERY-USER-1",
                    lines = listOf(
                        CreateExternalOrderLineRequest(menuId = latteMenuId, quantity = 1L),
                        CreateExternalOrderLineRequest(menuId = morningSetMenuId, quantity = 1L),
                    ),
                ),
            )
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.MENU_INGREDIENT_OUT_OF_STOCK)

        // 3. 강남점 사장님이 우유 재고를 10개로 재입고 등록
        partnerIngredientController.updateStockQuantity(
            storeId = gangnamStoreId,
            ingredientId = milkIngredientId,
            request = UpdateIngredientStockRequest(stockQuantity = 10),
        )

        // 4. 동일한 복합 주문 재시도 -> 정상 통과
        val recoveredOrderRes = externalOrderController.createExternalOrder(
            CreateExternalOrderRequest(
                storeId = gangnamStoreId,
                channel = OrderChannel.DELIVERY_PARTNER,
                externalCustomerKey = "DELIVERY-USER-1",
                lines = listOf(
                    CreateExternalOrderLineRequest(menuId = latteMenuId, quantity = 1L),
                    CreateExternalOrderLineRequest(menuId = morningSetMenuId, quantity = 1L),
                ),
            ),
        )
        assertThat(recoveredOrderRes.data?.orderKey).isNotBlank()
    }

    private fun createStore(name: String, branchCode: String): Store {
        val location = StoreLocation("서울 강남구", 37.5, 127.0)
        val salesInfo = SalesInformation(location, emptyList(), "02-123-4567")
        val busiInfo = BusinessInformation("홍길동", name, "123-45-67890", "서울 강남구")
        return Store(0L, name, branchCode, StoreStatus.OPEN, salesInfo, busiInfo)
    }

    private fun saveMenu(name: String, type: MenuType, costPrice: BigDecimal, salesPrice: BigDecimal): Long {
        val menu = MenuEntity(
            name = name,
            type = type,
            costPrice = costPrice,
            salesPrice = salesPrice,
            description = name,
        )
        menu.active()
        return menuJpaRepository.saveAndFlush(menu).id
    }
}
