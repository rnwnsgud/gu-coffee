package com.coffee.gu.payment.webhook.controller.v1

import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.payment.PaymentWebhookSyncAction
import com.coffee.gu.payment.PaymentWebhookSyncResult
import com.coffee.gu.payment.webhook.controller.PaymentWebhookRestDocsTest
import com.coffee.gu.payment.webhook.order.TossPaymentWebhookService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TossPaymentWebhookControllerTest : PaymentWebhookRestDocsTest() {

    private val tossPaymentWebhookService: TossPaymentWebhookService = mock()

    override val controller: Any = TossPaymentWebhookController(tossPaymentWebhookService)

    @Test
    @DisplayName("토스페이먼츠 결제 상태 변경 웹훅을 수신해 주문·결제 상태와 한정 수량 재고를 동기화한다")
    fun receiveTossPaymentWebhook() {
        val rawBody = """
            {
              "eventType": "PAYMENT_STATUS_CHANGED",
              "createdAt": "2026-11-18T18:10:00+09:00",
              "data": {
                "paymentKey": "tviva20261118181000",
                "orderId": "ORD-20261118-001",
                "status": "CANCELED",
                "method": "카드",
                "cancels": [
                  {
                    "transactionKey": "tx-cancel-999",
                    "cancelReason": "가맹점 관리자 직접 취소",
                    "cancelAmount": 7500
                  }
                ]
              }
            }
        """.trimIndent()

        given(
            tossPaymentWebhookService.handleWebhook(
                clientIp = any(),
                signature = eq("v1:sampleBase64Signature=="),
                transmissionTime = eq("2026-11-18T18:10:00.000000"),
                rawPayload = any(),
            ),
        ).willReturn(
            PaymentWebhookSyncResult(
                orderKey = "ORD-20261118-001",
                paymentState = PaymentState.CANCELED,
                orderState = OrderState.CANCELED,
                action = PaymentWebhookSyncAction.CANCELED,
            ),
        )

        mockMvc.perform(
            post("/v1/webhooks/payments/toss")
                .header(TossPaymentWebhookController.SIGNATURE_HEADER, "v1:sampleBase64Signature==")
                .header(TossPaymentWebhookController.TRANSMISSION_TIME_HEADER, "2026-11-18T18:10:00.000000")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawBody),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.orderKey").value("ORD-20261118-001"))
            .andExpect(jsonPath("$.data.paymentState").value("CANCELED"))
            .andExpect(jsonPath("$.data.orderState").value("CANCELED"))
            .andExpect(jsonPath("$.data.action").value("CANCELED"))
            .andDo(
                document(
                    identifier = "webhook-payment-toss",
                    tag = "Payment Webhook",
                    summary = "토스페이먼츠 결제 상태 변경 웹훅 수신 API",
                    description = "토스페이먼츠 웹훅(가상계좌 입금 완료, 상점관리자 직접 취소 등)을 수신해 결제·주문 상태 및 한정 수량 재고를 동기화합니다.",
                    requestHeaders(
                        headerWithName(TossPaymentWebhookController.SIGNATURE_HEADER)
                            .description("토스페이먼츠 웹훅 HMAC-SHA256 서명 (로컬 비활성화 가능)")
                            .optional(),
                        headerWithName(TossPaymentWebhookController.TRANSMISSION_TIME_HEADER)
                            .description("웹훅 전송 시각 헤더 (서명 검증 시 함께 사용)")
                            .optional(),
                    ),
                    requestFields(
                        fieldWithPath("eventType").type(JsonFieldType.STRING).description("웹훅 이벤트 타입 (PAYMENT_STATUS_CHANGED)"),
                        fieldWithPath("createdAt").type(JsonFieldType.STRING).description("이벤트 생성 시각").optional(),
                        fieldWithPath("data.paymentKey").type(JsonFieldType.STRING).description("토스페이먼츠 결제 키"),
                        fieldWithPath("data.orderId").type(JsonFieldType.STRING).description("가맹점 주문 키 (orderKey)"),
                        fieldWithPath("data.status").type(JsonFieldType.STRING).description("결제 상태 (DONE, CANCELED, ABORTED 등)"),
                        fieldWithPath("data.method").type(JsonFieldType.STRING).description("결제 수단 (카드, 가상계좌 등)").optional(),
                        fieldWithPath("data.cancels").type(JsonFieldType.ARRAY).description("취소 이력 목록").optional(),
                        fieldWithPath("data.cancels[].transactionKey").type(JsonFieldType.STRING).description("취소 트랜잭션 키").optional(),
                        fieldWithPath("data.cancels[].cancelReason").type(JsonFieldType.STRING).description("취소 사유").optional(),
                        fieldWithPath("data.cancels[].cancelAmount").type(JsonFieldType.NUMBER).description("취소 금액").optional(),
                    ),
                    responseFields(
                        fieldWithPath("status").type(JsonFieldType.STRING).description("요청 결과 상태"),
                        fieldWithPath("data.orderKey").type(JsonFieldType.STRING).description("동기화된 내부 주문 키"),
                        fieldWithPath("data.paymentState").type(JsonFieldType.STRING).description("동기화 후 결제 상태"),
                        fieldWithPath("data.orderState").type(JsonFieldType.STRING).description("동기화 후 주문 상태"),
                        fieldWithPath("data.action").type(JsonFieldType.STRING).description("처리 결과 액션 (PAID, CANCELED, FAILED, IGNORED_ALREADY_PROCESSED)"),
                        fieldWithPath("error").type(JsonFieldType.NULL).description("에러 정보 (성공 시 null)"),
                    ),
                ),
            )
    }
}
