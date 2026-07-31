package com.coffee.gu.api.controller.v1;

import com.coffee.gu.api.controller.RestDocsTest;
import com.coffee.gu.api.controller.v1.request.AddCartItemRequest;
import com.coffee.gu.api.controller.v1.request.ModifyCartItemRequest;
import com.coffee.gu.cart.Cart;
import com.coffee.gu.cart.CartService;
import com.coffee.gu.Principal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockitoSettings(strictness = Strictness.LENIENT)
class CartControllerTest extends RestDocsTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    @Override
    protected Object getController() {
        return cartController;
    }

    @Test
    @DisplayName("장바구니 조회 API")
    void getCart() throws Exception {
        // given
        Principal principal = Principal.user("1L");
        Cart cart = new Cart(principal, List.of());
        given(cartService.getCart(any())).willReturn(cart);

        // when & then
        mockMvc.perform(get("/v1/cart")
                        .header("Gu-Coffee-com.coffee.gu.Principal-Id", "1")
                        .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andDo(document("cart-get",
                        requestHeaders(
                                headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자"),
                                headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입 (USER/GUEST)")
                        ),
                        responseFields(
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("data.items").description("장바구니 아이템 목록"),
                                fieldWithPath("error").description("에러 정보 (정상 시 null)")
                        )
                ));
    }

    @Test
    @DisplayName("장바구니 아이템 추가 API")
    void addCartItem() throws Exception {
        // given
        AddCartItemRequest request = new AddCartItemRequest(100L, 2L);

        // when & then
        mockMvc.perform(post("/v1/cart/items")
                        .header("Gu-Coffee-com.coffee.gu.Principal-Id", "1")
                        .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andDo(document("cart-add-item",
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
                ));
    }

    @Test
    @DisplayName("장바구니 아이템 수정 API")
    void modifyCartItem() throws Exception {
        // given
        ModifyCartItemRequest request = new ModifyCartItemRequest(5L);

        // when & then
        mockMvc.perform(put("/v1/cart/items/{cartItemId}", 1L)
                        .header("Gu-Coffee-com.coffee.gu.Principal-Id", "1")
                        .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andDo(document("cart-modify-item",
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
                ));
    }

    @Test
    @DisplayName("장바구니 아이템 삭제 API")
    void deleteCartItem() throws Exception {
        // when & then
        mockMvc.perform(delete("/v1/cart/items/{cartItemId}", 1L)
                        .header("Gu-Coffee-com.coffee.gu.Principal-Id", "1")
                        .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andDo(document("cart-delete-item",
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
                ));
    }
}
