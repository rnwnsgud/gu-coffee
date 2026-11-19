package com.coffee.gu.payment.webhook.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.PaymentMethod
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class TossWebhookPayloadProcessorTest {

    private val processor = TossWebhookPayloadProcessor()

    @Test
    @DisplayName("DEPOSIT_CALLBACK 가상계좌 입금 완료(루트 레벨 JSON) 웹훅을 PaymentWebhookCommand로 변환한다")
    fun parseDepositCallbackWebhook() {
        val rawJson = """
            {
              "createdAt": "2026-11-18T18:10:00.000000",
              "secret": "tkC5xBUmjcYstSYXnrj4q",
              "status": "DONE",
              "transactionKey": "9FF15E1A29D0E77C218F57262BFA4986",
              "orderId": "ORD-20261118-001"
            }
        """.trimIndent()

        val command = processor.process(rawJson)

        assertThat(command.orderKey).isEqualTo("ORD-20261118-001")
        assertThat(command.paymentKey).isNull()
        assertThat(command.status).isEqualTo("DONE")
        assertThat(command.method).isEqualTo(PaymentMethod.VIRTUAL_ACCOUNT)
        assertThat(command.cancelTransactionKey).isEqualTo("9FF15E1A29D0E77C218F57262BFA4986")
    }

    @Test
    @DisplayName("PAYMENT_STATUS_CHANGED 상점관리자 직접 취소(CANCELED) 웹훅 JSON에서 취소 사유와 트랜잭션 키를 추출한다")
    fun parsePaymentStatusChangedCanceledWebhook() {
        val rawJson = """
            {
              "eventType": "PAYMENT_STATUS_CHANGED",
              "data": {
                "paymentKey": "tviva20261118181000",
                "orderId": "ORD-20261118-002",
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

        val command = processor.process(rawJson)

        assertThat(command.orderKey).isEqualTo("ORD-20261118-002")
        assertThat(command.paymentKey).isEqualTo("tviva20261118181000")
        assertThat(command.status).isEqualTo("CANCELED")
        assertThat(command.method).isEqualTo(PaymentMethod.CARD)
        assertThat(command.cancelTransactionKey).isEqualTo("tx-cancel-999")
        assertThat(command.cancelReason).isEqualTo("가맹점 관리자 직접 취소")
    }

    @Test
    @DisplayName("CANCEL_STATUS_CHANGED 웹훅 JSON에서 취소 완료 상태를 CANCELED 명령으로 변환한다")
    fun parseCancelStatusChangedWebhook() {
        val rawJson = """
            {
              "eventType": "CANCEL_STATUS_CHANGED",
              "data": {
                "paymentKey": "tviva20261118181000",
                "orderId": "ORD-20261118-003",
                "cancelStatus": "DONE",
                "transactionKey": "tx-cancel-async-1",
                "cancelReason": "비동기 취소 완료"
              }
            }
        """.trimIndent()

        val command = processor.process(rawJson)

        assertThat(command.orderKey).isEqualTo("ORD-20261118-003")
        assertThat(command.status).isEqualTo("CANCELED")
        assertThat(command.cancelTransactionKey).isEqualTo("tx-cancel-async-1")
        assertThat(command.cancelReason).isEqualTo("비동기 취소 완료")
    }

    @Test
    @DisplayName("필수 필드가 누락되었거나 지원하지 않는 eventType이면 INVALID_REQUEST 예외를 발생시킨다")
    fun rejectInvalidPayload() {
        val invalidJson = """{"eventType":"UNKNOWN_EVENT","data":{"orderId":""}}"""

        assertThatThrownBy {
            processor.process(invalidJson)
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_REQUEST)
    }
}
