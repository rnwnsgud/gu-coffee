package com.coffee.gu.api.controller.v1

import com.coffee.gu.api.controller.RestDocsTest
import com.coffee.gu.api.controller.v1.request.CreateExternalOrderLineRequest
import com.coffee.gu.api.controller.v1.request.CreateExternalOrderRequest
import com.coffee.gu.enums.OrderChannel
import com.coffee.gu.order.OrderService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.quality.Strictness
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@MockitoSettings(strictness = Strictness.LENIENT)
class ExternalOrderControllerTest : RestDocsTest() {

    private val objectMapper = ObjectMapper()

    @Mock
    private lateinit var orderService: OrderService

    @InjectMocks
    private lateinit var externalOrderController: ExternalOrderController

    override val controller: Any
        get() = externalOrderController

    @Test
    @DisplayName("매장 키오스크 주문 인입 API")
    fun createExternalOrder() {
        val request = CreateExternalOrderRequest(
            storeId = 1L,
            channel = OrderChannel.KIOSK,
            externalCustomerKey = "KIOSK-01",
            lines = listOf(
                CreateExternalOrderLineRequest(menuId = 6L, quantity = 1L),
            ),
        )
        given(orderService.create(any())).willReturn("0S6DXFAGW286R")

        mockMvc.perform(
            post("/v1/external-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.orderKey").value("0S6DXFAGW286R"))
            .andDo(
                document(
                    identifier = "external-order-create",
                    tag = "Order",
                    summary = "매장 키오스크 주문 인입 API",
                    snippets = arrayOf(
                        requestFields(
                            fieldWithPath("storeId").description("주문 대상 매장 식별자 (예: 1=강남본점, 2=판교역점)"),
                            fieldWithPath("channel").description("주문 유입 채널 (KIOSK 전용 — 배달 플랫폼은 웹훅 API 사용)"),
                            fieldWithPath("externalCustomerKey").description("매장 키오스크 단말/고객 식별 키"),
                            fieldWithPath("lines[].menuId").description("주문 메뉴 식별자 (예: 2=카페라떼, 6=모닝세트)"),
                            fieldWithPath("lines[].quantity").description("주문 수량"),
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
    @DisplayName("배달 플랫폼 채널(DELIVERY_PARTNER)로 외부 주문 API 호출 시 INVALID_REQUEST로 차단된다")
    fun rejectDeliveryPartnerChannel() {
        val request = CreateExternalOrderRequest(
            storeId = 1L,
            channel = OrderChannel.DELIVERY_PARTNER,
            externalCustomerKey = "BAEMIN-USER-01",
            lines = listOf(
                CreateExternalOrderLineRequest(menuId = 6L, quantity = 1L),
            ),
        )

        mockMvc.perform(
            post("/v1/external-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value("ERROR"))
    }
}
