package com.coffee.gu.storedevice.controller.v1

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.StoreDeviceType
import com.coffee.gu.storedevice.auth.StoreDeviceAuthenticator
import com.coffee.gu.storedevice.auth.StoreDevicePrincipal
import com.coffee.gu.storedevice.auth.StoreDevicePrincipalArgumentResolver
import com.coffee.gu.storedevice.controller.StoreDeviceRestDocsTest
import com.coffee.gu.storedevice.controller.v1.request.CreateDeviceOrderLineRequest
import com.coffee.gu.storedevice.controller.v1.request.CreateDeviceOrderRequest
import com.coffee.gu.storedevice.order.StoreDeviceOrderService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.quality.Strictness
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@MockitoSettings(strictness = Strictness.LENIENT)
class StoreDeviceOrderControllerTest : StoreDeviceRestDocsTest() {

    private val objectMapper = ObjectMapper()

    @Mock
    private lateinit var storeDeviceOrderService: StoreDeviceOrderService

    @Mock
    override lateinit var storeDeviceAuthenticator: StoreDeviceAuthenticator

    @InjectMocks
    private lateinit var storeDeviceOrderController: StoreDeviceOrderController

    override val controller: Any
        get() = storeDeviceOrderController

    @Test
    @DisplayName("매장 키오스크 디바이스 주문 생성 API")
    fun createDeviceOrder() {
        val device = StoreDevicePrincipal(
            deviceId = 1L,
            deviceCode = "KIOSK-GANGNAM-01",
            storeId = 1L,
            deviceType = StoreDeviceType.KIOSK,
        )
        val request = CreateDeviceOrderRequest(
            lines = listOf(
                CreateDeviceOrderLineRequest(menuId = 6L, quantity = 1L),
            ),
        )
        given(storeDeviceAuthenticator.authenticate("kiosk-gangnam-01-secret-key")).willReturn(device)
        given(storeDeviceOrderService.createOrder(any(), any())).willReturn("0S6DXFAGW286R")

        mockMvc.perform(
            post("/v1/devices/orders")
                .header(StoreDevicePrincipalArgumentResolver.DEVICE_API_KEY_HEADER, "kiosk-gangnam-01-secret-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.orderKey").value("0S6DXFAGW286R"))
            .andDo(
                document(
                    identifier = "device-order-create",
                    tag = "Store Device",
                    summary = "매장 기기(키오스크·POS) 주문 생성 API",
                    description = "매장 고정 기기 API Key로 인증된 키오스크 또는 POS 단말이 현장 주문을 생성합니다. 매장(storeId)과 채널(KIOSK/POS)은 인증된 기기 정보로부터 서버에서 자동 바인딩됩니다.",
                    snippets = arrayOf(
                        requestHeaders(
                            headerWithName(StoreDevicePrincipalArgumentResolver.DEVICE_API_KEY_HEADER)
                                .description("매장 고정 기기 API Key (예: kiosk-gangnam-01-secret-key)"),
                        ),
                        requestFields(
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
    @DisplayName("유효하지 않은 기기 API Key로 호출 시 401 UNAUTHORIZED를 반환한다")
    fun rejectUnauthorizedDevice() {
        val request = CreateDeviceOrderRequest(
            lines = listOf(CreateDeviceOrderLineRequest(menuId = 1L, quantity = 1L)),
        )
        given(storeDeviceAuthenticator.authenticate(null))
            .willThrow(CoreException(ErrorType.UNAUTHORIZED, "매장 기기 API Key 헤더가 누락되었습니다."))

        mockMvc.perform(
            post("/v1/devices/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value("ERROR"))
    }
}
