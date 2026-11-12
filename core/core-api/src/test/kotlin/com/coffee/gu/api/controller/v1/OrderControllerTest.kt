package com.coffee.gu.api.controller.v1

import com.coffee.gu.Principal
import com.coffee.gu.api.controller.RestDocsTest
import com.coffee.gu.api.controller.v1.request.CreateOrderFromCartRequest
import com.coffee.gu.api.controller.v1.request.CreateOrderRequest
import com.coffee.gu.cart.Cart
import com.coffee.gu.cart.CartItem
import com.coffee.gu.cart.CartService
import com.coffee.gu.coupon.IssuedCouponService
import com.coffee.gu.enums.MenuType
import com.coffee.gu.enums.OrderState
import com.coffee.gu.menu.Menu
import com.coffee.gu.menu.Price
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderLine
import com.coffee.gu.order.OrderService
import com.coffee.gu.order.OrderSummary
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.quality.Strictness
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal

@MockitoSettings(strictness = Strictness.LENIENT)
class OrderControllerTest : RestDocsTest() {

    private val objectMapper = ObjectMapper()

    @Mock
    private lateinit var orderService: OrderService

    @Mock
    private lateinit var cartService: CartService

    @Mock
    private lateinit var issuedCouponService: IssuedCouponService

    @InjectMocks
    private lateinit var orderController: OrderController

    override val controller: Any
        get() = orderController

    @Test
    @DisplayName("앱 즉시 주문 생성 API")
    fun createOrder() {
        val request = CreateOrderRequest(menuId = 6L, quantity = 1L, storeId = 1L)
        given(orderService.create(any())).willReturn("0S6DXFAGW286R")

        mockMvc.perform(
            post("/v1/orders")
                .header("Gu-Coffee-com.coffee.gu.Principal-Id", "U1")
                .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.orderKey").value("0S6DXFAGW286R"))
            .andDo(
                document(
                    identifier = "order-create",
                    tag = "Order",
                    summary = "앱 즉시 주문 생성 API",
                    snippets = arrayOf(
                        requestHeaders(
                            headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자 (예: U1)"),
                            headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입 (USER/GUEST)"),
                        ),
                        requestFields(
                            fieldWithPath("menuId").description("주문할 메뉴 식별자 (예: 1=아메리카노, 2=카페라떼, 6=모닝세트)"),
                            fieldWithPath("quantity").description("주문 수량"),
                            fieldWithPath("storeId").description("주문 매장 식별자 (예: 1=강남본점, 2=판교역점)"),
                        ),
                        responseFields(
                            fieldWithPath("status").description("응답 상태"),
                            fieldWithPath("data.orderKey").description("생성된 주문 고유 키 (TSID)"),
                            fieldWithPath("error").description("에러 정보 (정상 시 null)"),
                        ),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("장바구니 기반 주문 생성 API")
    fun createOrderFromCart() {
        val principal = Principal.user("1")
        val menu = Menu(
            id = 6L,
            name = "모닝세트 (카페라떼 + 베이글)",
            type = MenuType.DESSERT,
            price = Price(BigDecimal.valueOf(2500), BigDecimal.valueOf(7500)),
        )
        val cart = Cart(
            principal = principal,
            items = listOf(CartItem(id = 1L, menu = menu, quantity = 1L)),
        )
        val request = CreateOrderFromCartRequest(storeId = 1L, cartItemIds = setOf(1L))
        given(cartService.getCart(any())).willReturn(cart)
        given(orderService.create(any())).willReturn("0S6DXFAGW9999")

        mockMvc.perform(
            post("/v1/cart-orders")
                .header("Gu-Coffee-com.coffee.gu.Principal-Id", "U1")
                .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.orderKey").value("0S6DXFAGW9999"))
            .andDo(
                document(
                    identifier = "order-create-from-cart",
                    tag = "Order",
                    summary = "장바구니 기반 주문 생성 API",
                    snippets = arrayOf(
                        requestHeaders(
                            headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자 (예: U1)"),
                            headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입 (USER/GUEST)"),
                        ),
                        requestFields(
                            fieldWithPath("storeId").description("주문 매장 식별자 (예: 1=강남본점, 2=판교역점)"),
                            fieldWithPath("cartItemIds").description("주문 전환할 장바구니 아이템 ID 목록 (시드 데이터 기본값: [1])"),
                        ),
                        responseFields(
                            fieldWithPath("status").description("응답 상태"),
                            fieldWithPath("data.orderKey").description("생성된 주문 고유 키 (TSID)"),
                            fieldWithPath("error").description("에러 정보 (정상 시 null)"),
                        ),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("주문 체크아웃 조회 API")
    fun getOrderForCheckout() {
        val principal = Principal.user("1")
        val orderKey = "0S6DXFAGW286R"
        val line = OrderLine(
            orderKey = orderKey,
            menuId = 6L,
            menuName = "모닝세트 (카페라떼 + 베이글)",
            imageUrl = "https://images.unsplash.com/photo-1509440159596-0249088772ff",
            description = "카페라떼 1잔과 플레인 베이글 1개로 구성된 든든한 모닝 세트",
            quantity = 1L,
            unitPrice = BigDecimal.valueOf(7500),
            totalPrice = BigDecimal.valueOf(7500),
            isStampEligible = true,
        )
        val order = Order(
            key = orderKey,
            name = "모닝세트 (카페라떼 + 베이글)",
            principal = principal,
            storeId = 1L,
            totalPrice = BigDecimal.valueOf(7500),
            state = OrderState.CREATED,
            lines = listOf(line),
        )
        given(orderService.getOrder(eq(orderKey), eq(OrderState.CREATED))).willReturn(order)
        given(issuedCouponService.getIssuedCouponsForCheckout(any(), any())).willReturn(emptyList())

        mockMvc.perform(
            get("/v1/orders/{orderKey}/checkout", orderKey)
                .header("Gu-Coffee-com.coffee.gu.Principal-Id", "U1")
                .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andDo(
                document(
                    identifier = "order-checkout",
                    tag = "Order",
                    summary = "주문 체크아웃 조회 API",
                    snippets = arrayOf(
                        requestHeaders(
                            headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자 (예: U1)"),
                            headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입 (USER/GUEST)"),
                        ),
                        pathParameters(
                            parameterWithName("orderKey").description("주문 고유 키"),
                        ),
                        responseFields(
                            fieldWithPath("status").description("응답 상태"),
                            fieldWithPath("data.key").description("주문 고유 키"),
                            fieldWithPath("data.name").description("주문 대표 명칭"),
                            fieldWithPath("data.totalPrice").description("총 주문 금액"),
                            fieldWithPath("data.lines[].menuId").description("메뉴 식별자"),
                            fieldWithPath("data.lines[].menuName").description("메뉴 명칭"),
                            fieldWithPath("data.lines[].imageUrl").description("메뉴 이미지 URL"),
                            fieldWithPath("data.lines[].description").description("메뉴 설명"),
                            fieldWithPath("data.lines[].quantity").description("주문 수량"),
                            fieldWithPath("data.lines[].unitPrice").description("단가"),
                            fieldWithPath("data.lines[].totalPrice").description("합계 금액"),
                            fieldWithPath("data.usableCoupons").description("적용 가능한 보유 쿠폰 목록"),
                            fieldWithPath("error").description("에러 정보 (정상 시 null)"),
                        ),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("결제 완료된 주문 목록 조회 API")
    fun getOrders() {
        val principal = Principal.user("1")
        val summaries = listOf(
            OrderSummary(
                key = "0S6DXFAGW286R",
                name = "모닝세트 (카페라떼 + 베이글)",
                principal = principal,
                totalPrice = BigDecimal.valueOf(7500),
                state = OrderState.PAID,
            ),
        )
        given(orderService.getPaidOrders(any())).willReturn(summaries)

        mockMvc.perform(
            get("/v1/orders")
                .header("Gu-Coffee-com.coffee.gu.Principal-Id", "U1")
                .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andDo(
                document(
                    identifier = "order-list",
                    tag = "Order",
                    summary = "결제 완료된 주문 목록 조회 API",
                    snippets = arrayOf(
                        requestHeaders(
                            headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자 (예: U1)"),
                            headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입 (USER/GUEST)"),
                        ),
                        responseFields(
                            fieldWithPath("status").description("응답 상태"),
                            fieldWithPath("data[].key").description("주문 고유 키"),
                            fieldWithPath("data[].name").description("주문 대표 명칭"),
                            fieldWithPath("data[].totalPrice").description("총 결제 금액"),
                            fieldWithPath("data[].state").description("주문 상태 (PAID)"),
                            fieldWithPath("error").description("에러 정보 (정상 시 null)"),
                        ),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("단건 주문 상세 조회 API")
    fun getOrder() {
        val principal = Principal.user("1")
        val orderKey = "0S6DXFAGW286R"
        val line = OrderLine(
            orderKey = orderKey,
            menuId = 6L,
            menuName = "모닝세트 (카페라떼 + 베이글)",
            imageUrl = "https://images.unsplash.com/photo-1509440159596-0249088772ff",
            description = "카페라떼 1잔과 플레인 베이글 1개로 구성된 든든한 모닝 세트",
            quantity = 1L,
            unitPrice = BigDecimal.valueOf(7500),
            totalPrice = BigDecimal.valueOf(7500),
            isStampEligible = true,
        )
        val order = Order(
            key = orderKey,
            name = "모닝세트 (카페라떼 + 베이글)",
            principal = principal,
            storeId = 1L,
            totalPrice = BigDecimal.valueOf(7500),
            state = OrderState.PAID,
            lines = listOf(line),
        )
        given(orderService.getOrder(eq(orderKey), eq(OrderState.PAID))).willReturn(order)

        mockMvc.perform(
            get("/v1/orders/{orderKey}", orderKey)
                .header("Gu-Coffee-com.coffee.gu.Principal-Id", "U1")
                .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andDo(
                document(
                    identifier = "order-detail",
                    tag = "Order",
                    summary = "단건 주문 상세 조회 API",
                    snippets = arrayOf(
                        requestHeaders(
                            headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자 (예: U1)"),
                            headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입 (USER/GUEST)"),
                        ),
                        pathParameters(
                            parameterWithName("orderKey").description("주문 고유 키"),
                        ),
                        responseFields(
                            fieldWithPath("status").description("응답 상태"),
                            fieldWithPath("data.key").description("주문 고유 키"),
                            fieldWithPath("data.name").description("주문 대표 명칭"),
                            fieldWithPath("data.totalPrice").description("총 결제 금액"),
                            fieldWithPath("data.state").description("주문 상태"),
                            fieldWithPath("data.lines[].menuId").description("메뉴 식별자"),
                            fieldWithPath("data.lines[].menuName").description("메뉴 명칭"),
                            fieldWithPath("data.lines[].imageUrl").description("메뉴 이미지 URL"),
                            fieldWithPath("data.lines[].description").description("메뉴 설명"),
                            fieldWithPath("data.lines[].quantity").description("주문 수량"),
                            fieldWithPath("data.lines[].unitPrice").description("단가"),
                            fieldWithPath("data.lines[].totalPrice").description("합계 금액"),
                            fieldWithPath("error").description("에러 정보 (정상 시 null)"),
                        ),
                    ),
                ),
            )
    }
}
