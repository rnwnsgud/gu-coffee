package com.coffee.gu.webhook.controller.v1

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.webhook.controller.WebhookRestDocsTest
import com.coffee.gu.webhook.order.ExternalWebhookService
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
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@MockitoSettings(strictness = Strictness.LENIENT)
class ExternalWebhookControllerTest : WebhookRestDocsTest() {

    @Mock
    private lateinit var externalWebhookService: ExternalWebhookService

    @InjectMocks
    private lateinit var externalWebhookController: ExternalWebhookController

    override val controller: Any
        get() = externalWebhookController

    @Test
    @DisplayName("배달의민족 주문 웹훅 수신 API")
    fun receiveBaeminOrder() {
        val idempotencyKey = "BM-WEBHOOK-20261116-001"
        val signature = "sha256=8f2a9c4b1e3d5f7a9c0b2e4d6f8a1c3e5b7d9f0a2c4e6b8d0f1a3c5e7b9d1f3a"
        val rawPayload = """
            {
              "orderNo": "BM-ORDER-1001",
              "shopNo": 1,
              "memberKey": "BAEMIN-USER-01",
              "menus": [
                {
                  "menuCode": "BM-MORNING-SET",
                  "count": 1
                }
              ]
            }
        """.trimIndent()

        given(
            externalWebhookService.receiveBaeminOrder(
                idempotencyKey = eq(idempotencyKey),
                signature = eq(signature),
                clientIp = any(),
                rawPayload = eq(rawPayload),
            ),
        ).willReturn("01JBMWEBHOOKORDERKEY00001")

        mockMvc.perform(
            post("/v1/webhooks/orders/baemin")
                .header(ExternalWebhookController.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .header(ExternalWebhookController.SIGNATURE_HEADER, signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawPayload),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.orderKey").value("01JBMWEBHOOKORDERKEY00001"))
            .andExpect(jsonPath("$.data.platform").value("BAEMIN"))
            .andExpect(jsonPath("$.data.idempotencyKey").value(idempotencyKey))
            .andDo(
                document(
                    identifier = "webhook-order-baemin",
                    tag = "External Webhook",
                    summary = "배달의민족 주문 웹훅 수신 API",
                    description = "배달의민족 플랫폼의 주문 웹훅을 수신하여 IP 화이트리스트, HMAC-SHA256 서명, 멱등성 키 검증 및 외부 메뉴 코드(ACL) 변환 후 주문을 생성합니다.",
                    snippets = arrayOf(
                        requestHeaders(
                            headerWithName(ExternalWebhookController.IDEMPOTENCY_KEY_HEADER).description("웹훅 멱등성 키 (재전송 시 중복 주문 생성 방지)"),
                            headerWithName(ExternalWebhookController.SIGNATURE_HEADER).description("요청 바디 원문에 대한 플랫폼 HMAC-SHA256 서명"),
                        ),
                        requestFields(
                            fieldWithPath("orderNo").description("배달의민족 외부 주문 번호"),
                            fieldWithPath("shopNo").description("수신 대상 매장 식별자 (예: 1=강남본점)"),
                            fieldWithPath("memberKey").description("배달의민족 주문 고객 식별 키"),
                            fieldWithPath("menus").description("주문 메뉴 목록"),
                            fieldWithPath("menus[].menuCode").description("배달의민족 외부 메뉴 코드 (예: BM-AMERICANO, BM-LATTE, BM-MORNING-SET)"),
                            fieldWithPath("menus[].count").description("주문 수량"),
                        ),
                        responseFields(
                            fieldWithPath("status").description("응답 상태"),
                            fieldWithPath("data.orderKey").description("생성(또는 멱등 조회)된 내부 주문 키"),
                            fieldWithPath("data.platform").description("연동 배달 플랫폼 (BAEMIN)"),
                            fieldWithPath("data.idempotencyKey").description("처리된 웹훅 멱등성 키"),
                            fieldWithPath("error").description("에러 정보 (정상 시 null)"),
                        ),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("쿠팡이츠 주문 웹훅 수신 API")
    fun receiveCoupangEatsOrder() {
        val idempotencyKey = "CE-WEBHOOK-20261116-001"
        val signature = "sha256=4b1e3d5f7a9c0b2e4d6f8a1c3e5b7d9f0a2c4e6b8d0f1a3c5e7b9d1f3a8f2a9c"
        val rawPayload = """
            {
              "externalOrderId": "CE-ORDER-2002",
              "merchantStoreId": 1,
              "buyerId": "COUPANG-USER-99",
              "orderItems": [
                {
                  "productCode": "CE-LATTE",
                  "qty": 2
                }
              ]
            }
        """.trimIndent()

        given(
            externalWebhookService.receiveCoupangEatsOrder(
                idempotencyKey = eq(idempotencyKey),
                signature = eq(signature),
                clientIp = any(),
                rawPayload = eq(rawPayload),
            ),
        ).willReturn("01JCEWEBHOOKORDERKEY00002")

        mockMvc.perform(
            post("/v1/webhooks/orders/coupang-eats")
                .header(ExternalWebhookController.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .header(ExternalWebhookController.SIGNATURE_HEADER, signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawPayload),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.orderKey").value("01JCEWEBHOOKORDERKEY00002"))
            .andExpect(jsonPath("$.data.platform").value("COUPANG_EATS"))
            .andExpect(jsonPath("$.data.idempotencyKey").value(idempotencyKey))
            .andDo(
                document(
                    identifier = "webhook-order-coupang-eats",
                    tag = "External Webhook",
                    summary = "쿠팡이츠 주문 웹훅 수신 API",
                    description = "쿠팡이츠 플랫폼의 주문 웹훅을 수신하여 IP 화이트리스트, HMAC-SHA256 서명, 멱등성 키 검증 및 외부 상품 코드(ACL) 변환 후 주문을 생성합니다.",
                    snippets = arrayOf(
                        requestHeaders(
                            headerWithName(ExternalWebhookController.IDEMPOTENCY_KEY_HEADER).description("웹훅 멱등성 키 (재전송 시 중복 주문 생성 방지)"),
                            headerWithName(ExternalWebhookController.SIGNATURE_HEADER).description("요청 바디 원문에 대한 플랫폼 HMAC-SHA256 서명"),
                        ),
                        requestFields(
                            fieldWithPath("externalOrderId").description("쿠팡이츠 외부 주문 번호"),
                            fieldWithPath("merchantStoreId").description("수신 대상 매장 식별자 (예: 1=강남본점)"),
                            fieldWithPath("buyerId").description("쿠팡이츠 구매자 식별 키"),
                            fieldWithPath("orderItems").description("주문 상품 목록"),
                            fieldWithPath("orderItems[].productCode").description("쿠팡이츠 외부 상품 코드 (예: CE-AMERICANO, CE-LATTE, CE-MORNING-SET)"),
                            fieldWithPath("orderItems[].qty").description("주문 수량"),
                        ),
                        responseFields(
                            fieldWithPath("status").description("응답 상태"),
                            fieldWithPath("data.orderKey").description("생성(또는 멱등 조회)된 내부 주문 키"),
                            fieldWithPath("data.platform").description("연동 배달 플랫폼 (COUPANG_EATS)"),
                            fieldWithPath("data.idempotencyKey").description("처리된 웹훅 멱등성 키"),
                            fieldWithPath("error").description("에러 정보 (정상 시 null)"),
                        ),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("웹훅 HMAC 서명 검증 실패 시 401 UNAUTHORIZED를 반환한다")
    fun unauthorizedWhenWebhookSignatureInvalid() {
        val rawPayload = """{"orderNo":"BM-1","shopNo":1,"memberKey":"U1","menus":[{"menuCode":"BM-LATTE","count":1}]}"""
        given(
            externalWebhookService.receiveBaeminOrder(
                idempotencyKey = eq("IDEMP-1"),
                signature = eq("invalid-signature"),
                clientIp = any(),
                rawPayload = eq(rawPayload),
            ),
        ).willThrow(CoreException(ErrorType.UNAUTHORIZED, "유효하지 않은 웹훅 서명입니다."))

        mockMvc.perform(
            post("/v1/webhooks/orders/baemin")
                .header(ExternalWebhookController.IDEMPOTENCY_KEY_HEADER, "IDEMP-1")
                .header(ExternalWebhookController.SIGNATURE_HEADER, "invalid-signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawPayload),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.error.code").value("E401"))
    }
}
