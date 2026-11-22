package com.coffee.gu.storedevice.controller.v1

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderChannel
import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.StoreDeviceType
import com.coffee.gu.order.Order
import com.coffee.gu.storedevice.auth.StoreDeviceAuthenticator
import com.coffee.gu.storedevice.auth.StoreDevicePrincipal
import com.coffee.gu.storedevice.auth.StoreDevicePrincipalArgumentResolver
import com.coffee.gu.storedevice.controller.StoreDeviceRestDocsTest
import com.coffee.gu.storedevice.pos.PosOrderService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.quality.Strictness
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@MockitoSettings(strictness = Strictness.LENIENT)
class PosOrderControllerTest : StoreDeviceRestDocsTest() {

    @Mock
    private lateinit var posOrderService: PosOrderService

    @Mock
    override lateinit var storeDeviceAuthenticator: StoreDeviceAuthenticator

    @InjectMocks
    private lateinit var posOrderController: PosOrderController

    override val controller: Any
        get() = posOrderController

    private val posDevice = StoreDevicePrincipal(
        deviceId = 3L,
        deviceCode = "POS-GANGNAM-01",
        storeId = 1L,
        deviceType = StoreDeviceType.POS,
    )

    @Test
    @DisplayName("POS 매장 주문 목록 조회 API")
    fun findStoreOrders() {
        val orders = listOf(
            Order(
                key = "0S6DXFAGW286R",
                name = "모닝세트 (카페라떼 + 베이글)",
                principal = Principal.guest("POS-GANGNAM-01"),
                storeId = 1L,
                totalPrice = BigDecimal("7500.00"),
                state = OrderState.PAID,
                lines = emptyList(),
                channel = OrderChannel.POS,
            ),
        )
        given(storeDeviceAuthenticator.authenticate("pos-gangnam-01-secret-key")).willReturn(posDevice)
        given(posOrderService.findStoreOrders(any(), anyOrNull())).willReturn(orders)

        mockMvc.perform(
            get("/v1/devices/pos/orders")
                .header(StoreDevicePrincipalArgumentResolver.DEVICE_API_KEY_HEADER, "pos-gangnam-01-secret-key")
                .queryParam("state", "PAID"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data[0].orderKey").value("0S6DXFAGW286R"))
            .andExpect(jsonPath("$.data[0].channel").value("POS"))
            .andDo(
                document(
                    identifier = "pos-order-list",
                    tag = "Store Device",
                    summary = "POS 매장 주문 목록 조회 API",
                    description = "POS 기기 API Key로 인증된 매장의 주문 목록을 조회합니다. state 쿼리 파라미터로 주문 상태별 필터링이 가능합니다.",
                    snippets = arrayOf(
                        requestHeaders(
                            headerWithName(StoreDevicePrincipalArgumentResolver.DEVICE_API_KEY_HEADER)
                                .description("매장 POS 기기 API Key (예: pos-gangnam-01-secret-key)"),
                        ),
                        queryParameters(
                            parameterWithName("state").description("주문 상태 필터 (CREATED, PAID, CANCELED — 미입력 시 전체 조회)").optional(),
                        ),
                        responseFields(
                            fieldWithPath("status").description("응답 상태"),
                            fieldWithPath("data[].orderKey").description("주문 고유 키 (TSID)"),
                            fieldWithPath("data[].name").description("주문 대표명"),
                            fieldWithPath("data[].storeId").description("매장 식별자"),
                            fieldWithPath("data[].totalPrice").description("주문 총 금액"),
                            fieldWithPath("data[].state").description("주문 상태 (CREATED, PAID, CANCELED)"),
                            fieldWithPath("data[].channel").description("주문 유입 채널 (APP, CART, DELIVERY_PARTNER, KIOSK, POS)"),
                            fieldWithPath("error").description("에러 정보 (정상 시 null)"),
                        ),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("POS 매장 주문 취소 API")
    fun cancelStoreOrder() {
        val canceledOrder = Order(
            key = "0S6DXFAGW286R",
            name = "모닝세트 (카페라떼 + 베이글)",
            principal = Principal.guest("POS-GANGNAM-01"),
            storeId = 1L,
            totalPrice = BigDecimal("7500.00"),
            state = OrderState.CANCELED,
            lines = emptyList(),
            channel = OrderChannel.POS,
        )
        given(storeDeviceAuthenticator.authenticate("pos-gangnam-01-secret-key")).willReturn(posDevice)
        given(posOrderService.cancelStoreOrder(any(), any())).willReturn(canceledOrder)

        mockMvc.perform(
            post("/v1/devices/pos/orders/{orderKey}/cancel", "0S6DXFAGW286R")
                .header(StoreDevicePrincipalArgumentResolver.DEVICE_API_KEY_HEADER, "pos-gangnam-01-secret-key"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.orderKey").value("0S6DXFAGW286R"))
            .andExpect(jsonPath("$.data.state").value("CANCELED"))
            .andDo(
                document(
                    identifier = "pos-order-cancel",
                    tag = "Store Device",
                    summary = "POS 매장 주문 취소 API",
                    description = "POS 기기 API Key로 인증된 매장의 주문을 취소합니다. 결제 전 주문(CREATED)은 즉시 취소되며, 결제 완료 주문(PAID)은 PG 환불 및 롤백이 함께 수행됩니다.",
                    snippets = arrayOf(
                        requestHeaders(
                            headerWithName(StoreDevicePrincipalArgumentResolver.DEVICE_API_KEY_HEADER)
                                .description("매장 POS 기기 API Key (예: pos-gangnam-01-secret-key)"),
                        ),
                        pathParameters(
                            parameterWithName("orderKey").description("취소할 주문 고유 키 (TSID)"),
                        ),
                        responseFields(
                            fieldWithPath("status").description("응답 상태"),
                            fieldWithPath("data.orderKey").description("취소된 주문 고유 키"),
                            fieldWithPath("data.state").description("변경된 주문 상태 (CANCELED)"),
                            fieldWithPath("error").description("에러 정보 (정상 시 null)"),
                        ),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("KIOSK 기기로 POS 주문 취소 API 호출 시 403 FORBIDDEN을 반환한다")
    fun rejectKioskDeviceOnPosEndpoint() {
        val kioskDevice = StoreDevicePrincipal(
            deviceId = 1L,
            deviceCode = "KIOSK-GANGNAM-01",
            storeId = 1L,
            deviceType = StoreDeviceType.KIOSK,
        )
        given(storeDeviceAuthenticator.authenticate("kiosk-gangnam-01-secret-key")).willReturn(kioskDevice)
        given(posOrderService.cancelStoreOrder(any(), any()))
            .willThrow(CoreException(ErrorType.FORBIDDEN, "POS 기기에서만 사용할 수 있는 기능입니다."))

        mockMvc.perform(
            post("/v1/devices/pos/orders/{orderKey}/cancel", "0S6DXFAGW286R")
                .header(StoreDevicePrincipalArgumentResolver.DEVICE_API_KEY_HEADER, "kiosk-gangnam-01-secret-key"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value("ERROR"))
    }
}
