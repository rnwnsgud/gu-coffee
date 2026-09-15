package com.coffee.gu.api.controller.v1

import com.coffee.gu.Principal
import com.coffee.gu.api.controller.RestDocsTest
import com.coffee.gu.api.controller.v1.request.AddCartItemRequest
import com.coffee.gu.api.controller.v1.request.ModifyCartItemRequest
import com.coffee.gu.cart.Cart
import com.coffee.gu.cart.CartService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@MockitoSettings(strictness = Strictness.LENIENT)
class CartControllerTest : RestDocsTest() {

    private val objectMapper = ObjectMapper()

    @Mock
    private lateinit var cartService: CartService

    @InjectMocks
    private lateinit var cartController: CartController

    override val controller: Any
        get() = cartController

    @Test
    @DisplayName("장바구니 조회 API")
    fun getCart() {
        // given
        val principal = Principal.user("1L")
        val cart = Cart(principal, emptyList())
        given(cartService.getCart(any())).willReturn(cart)

        // when & then
        mockMvc.perform(
            get("/v1/cart")
                .header("Gu-Coffee-com.coffee.gu.Principal-Id", "1")
                .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andDo(
                document(
                    "cart-get",
                    requestHeaders(
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자"),
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입 (USER/GUEST)")
                    ),
                    responseFields(
                        fieldWithPath("status").description("응답 상태"),
                        fieldWithPath("data.items").description("장바구니 아이템 목록"),
                        fieldWithPath("error").description("에러 정보 (정상 시 null)")
                    )
                )
            )
    }

    @Test
    @DisplayName("장바구니 아이템 추가 API")
    fun addCartItem() {
        // given
        val request = AddCartItemRequest(100L, 2L)

        // when & then
        mockMvc.perform(
            post("/v1/cart/items")
                .header("Gu-Coffee-com.coffee.gu.Principal-Id", "1")
                .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andDo(
                document(
                    "cart-add-item",
                    requestHeaders(
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자"),
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입 (USER/GUEST)")
                    ),
                    requestFields(
                        fieldWithPath("menuId").description("메뉴 식별자"),
                        fieldWithPath("quantity").description("수량")
                    ),
                    responseFields(
                        fieldWithPath("status").description("응답 상태"),
                        fieldWithPath("data").description("응답 데이터 (null)"),
                        fieldWithPath("error").description("에러 정보 (정상 시 null)")
                    )
                )
            )
    }

    @Test
    @DisplayName("장바구니 아이템 수정 API")
    fun modifyCartItem() {
        // given
        val request = ModifyCartItemRequest(5L)

        // when & then
        mockMvc.perform(
            put("/v1/cart/items/{cartItemId}", 1L)
                .header("Gu-Coffee-com.coffee.gu.Principal-Id", "1")
                .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andDo(
                document(
                    "cart-modify-item",
                    requestHeaders(
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자"),
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입 (USER/GUEST)")
                    ),
                    pathParameters(
                        parameterWithName("cartItemId").description("장바구니 아이템 식별자")
                    ),
                    requestFields(
                        fieldWithPath("quantity").description("변경할 수량")
                    ),
                    responseFields(
                        fieldWithPath("status").description("응답 상태"),
                        fieldWithPath("data").description("응답 데이터 (null)"),
                        fieldWithPath("error").description("에러 정보 (정상 시 null)")
                    )
                )
            )
    }

    @Test
    @DisplayName("장바구니 아이템 삭제 API")
    fun deleteCartItem() {
        // when & then
        mockMvc.perform(
            delete("/v1/cart/items/{cartItemId}", 1L)
                .header("Gu-Coffee-com.coffee.gu.Principal-Id", "1")
                .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andDo(
                document(
                    "cart-delete-item",
                    requestHeaders(
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자"),
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입 (USER/GUEST)")
                    ),
                    pathParameters(
                        parameterWithName("cartItemId").description("장바구니 아이템 식별자")
                    ),
                    responseFields(
                        fieldWithPath("status").description("응답 상태"),
                        fieldWithPath("data").description("응답 데이터 (null)"),
                        fieldWithPath("error").description("에러 정보 (정상 시 null)")
                    )
                )
            )
    }
}
