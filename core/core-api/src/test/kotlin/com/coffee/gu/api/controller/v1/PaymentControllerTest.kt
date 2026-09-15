package com.coffee.gu.api.controller.v1

import com.coffee.gu.Principal
import com.coffee.gu.api.controller.RestDocsTest
import com.coffee.gu.api.controller.v1.request.CreatePaymentRequest
import com.coffee.gu.coupon.IssuedCouponService
import com.coffee.gu.enums.OrderState
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderLine
import com.coffee.gu.order.OrderService
import com.coffee.gu.payment.PaymentApprovalResult
import com.coffee.gu.payment.PaymentService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.OffsetDateTime

@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentControllerTest : RestDocsTest() {

    @Mock
    private lateinit var paymentService: PaymentService

    @Mock
    private lateinit var orderService: OrderService

    @Mock
    private lateinit var issuedCouponService: IssuedCouponService

    @InjectMocks
    private lateinit var paymentController: PaymentController

    private val objectMapper = ObjectMapper()

    override val controller: Any
        get() = paymentController

    @Test
    @DisplayName("결제 생성 API")
    fun createPayment() {
        // given
        val request = CreatePaymentRequest("order-key", null)
        val orderLine = OrderLine(1L, "order-key", 1L, "Coffee", "img", "desc", 1L, BigDecimal.valueOf(2000), BigDecimal.valueOf(2000), true)
        val order = Order("order-key", "Coffee", Principal.user("1"), 1L, BigDecimal.valueOf(2000), OrderState.CREATED, listOf(orderLine))

        given(orderService.getOrder(eq("order-key"), eq(OrderState.CREATED))).willReturn(order)
        given(issuedCouponService.getIssuedCouponsForCheckout(any(), any())).willReturn(emptyList())
        given(paymentService.createPayment(any(), any())).willReturn(10L)

        // when & then
        mockMvc.perform(
            post("/v1/payments")
                .header("Gu-Coffee-com.coffee.gu.Principal-Id", "U1")
                .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andDo(
                document(
                    "payment-create",
                    requestHeaders(
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자"),
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입")
                    ),
                    requestFields(
                        fieldWithPath("orderKey").description("주문 키"),
                        fieldWithPath("usedIssuedCouponId").description("사용할 쿠폰 식별자 (없으면 null)").optional()
                    ),
                    responseFields(
                        fieldWithPath("status").description("응답 상태"),
                        fieldWithPath("data.paymentId").description("생성된 결제 식별자"),
                        fieldWithPath("error").description("에러 정보")
                    )
                )
            )
    }

    @Test
    @DisplayName("결제 승인 API")
    fun confirm() {
        // given
        val order = Order("order-key", "Coffee", Principal.user("1"), 1L, BigDecimal.valueOf(2000), OrderState.PAID, emptyList())
        given(orderService.getOrder(eq(order.key))).willReturn(order)
        given(paymentService.approvePayment(any())).willReturn(PaymentApprovalResult.approved("order-key", "payment-key", OffsetDateTime.now()))

        // when & then
        mockMvc.perform(
            post("/v1/payments/confirm")
                .header("Gu-Coffee-com.coffee.gu.Principal-Id", "U1")
                .header("Gu-Coffee-com.coffee.gu.Principal-Type", "USER")
                .queryParam("orderId", "order-key")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andDo(
                document(
                    "payment-confirm",
                    requestHeaders(
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Id").description("사용자 식별자"),
                        headerWithName("Gu-Coffee-com.coffee.gu.Principal-Type").description("사용자 타입")
                    ),
                    queryParameters(
                        parameterWithName("orderId").description("주문 키")
                    ),
                    responseFields(
                        fieldWithPath("status").description("응답 상태"),
                        fieldWithPath("data.result").description("결제 상태"),
                        fieldWithPath("error").description("에러 정보")
                    )
                )
            )
    }
}
