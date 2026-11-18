package com.coffee.gu.webhook.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.DeliveryPlatform
import com.coffee.gu.enums.OrderChannel
import com.coffee.gu.webhook.ExternalMenuMapping
import com.coffee.gu.webhook.ExternalMenuMappingRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock

class WebhookPayloadProcessorTest {

    private val externalMenuMappingRepository: ExternalMenuMappingRepository = mock()
    private val externalMenuMappingFinder = ExternalMenuMappingFinder(externalMenuMappingRepository)
    private val processor = WebhookPayloadProcessor(externalMenuMappingFinder)

    @Test
    @DisplayName("배달의민족 웹훅 JSON과 쿠팡이츠 웹훅 JSON을 각각 외부 메뉴 매핑을 거쳐 내부 표준 NewOrder로 변환한다")
    fun translatePlatformSpecificPayloadsToNewOrder() {
        given(
            externalMenuMappingRepository.findAllByPlatformAndExternalMenuCodeIn(
                DeliveryPlatform.BAEMIN,
                setOf("BM-AMERICANO", "BM-MORNING-SET"),
            ),
        ).willReturn(
            listOf(
                ExternalMenuMapping(id = 1, platform = DeliveryPlatform.BAEMIN, externalMenuCode = "BM-AMERICANO", menuId = 1L),
                ExternalMenuMapping(id = 2, platform = DeliveryPlatform.BAEMIN, externalMenuCode = "BM-MORNING-SET", menuId = 6L),
            ),
        )

        given(
            externalMenuMappingRepository.findAllByPlatformAndExternalMenuCodeIn(
                DeliveryPlatform.COUPANG_EATS,
                setOf("CE-LATTE"),
            ),
        ).willReturn(
            listOf(
                ExternalMenuMapping(id = 3, platform = DeliveryPlatform.COUPANG_EATS, externalMenuCode = "CE-LATTE", menuId = 2L),
            ),
        )

        val baeminJson = """
            {
              "orderNo": "BM-20261116-001",
              "shopNo": 1,
              "memberKey": "USER-BM-99",
              "menus": [
                { "menuCode": "BM-AMERICANO", "count": 2 },
                { "menuCode": "BM-MORNING-SET", "count": 1 }
              ]
            }
        """.trimIndent()

        val baeminOrder = processor.processBaeminPayload(baeminJson)
        assertThat(baeminOrder.storeId).isEqualTo(1L)
        assertThat(baeminOrder.channel).isEqualTo(OrderChannel.DELIVERY_PARTNER)
        assertThat(baeminOrder.principal.key).isEqualTo("GBAEMIN:USER-BM-99")
        assertThat(baeminOrder.lines).hasSize(2)
        assertThat(baeminOrder.lines[0].menuId).isEqualTo(1L)
        assertThat(baeminOrder.lines[0].quantity).isEqualTo(2L)
        assertThat(baeminOrder.lines[1].menuId).isEqualTo(6L)
        assertThat(baeminOrder.lines[1].quantity).isEqualTo(1L)

        val coupangJson = """
            {
              "externalOrderId": "CE-20261116-777",
              "merchantStoreId": 2,
              "buyerId": "BUYER-CE-55",
              "orderItems": [
                { "productCode": "CE-LATTE", "qty": 3 }
              ]
            }
        """.trimIndent()

        val coupangOrder = processor.processCoupangEatsPayload(coupangJson)
        assertThat(coupangOrder.storeId).isEqualTo(2L)
        assertThat(coupangOrder.channel).isEqualTo(OrderChannel.DELIVERY_PARTNER)
        assertThat(coupangOrder.principal.key).isEqualTo("GCOUPANG_EATS:BUYER-CE-55")
        assertThat(coupangOrder.lines).hasSize(1)
        assertThat(coupangOrder.lines[0].menuId).isEqualTo(2L)
        assertThat(coupangOrder.lines[0].quantity).isEqualTo(3L)
    }

    @Test
    @DisplayName("매핑 테이블에 등록되지 않은 외부 플랫폼 메뉴 코드가 포함되면 MENU_MISMATCH_IN_ORDER 예외가 발생한다")
    fun throwMenuMismatchWhenExternalMenuCodeNotMapped() {
        given(
            externalMenuMappingRepository.findAllByPlatformAndExternalMenuCodeIn(
                DeliveryPlatform.BAEMIN,
                setOf("BM-UNKNOWN-MENU"),
            ),
        ).willReturn(emptyList())

        val json = """
            {
              "orderNo": "BM-002",
              "shopNo": 1,
              "memberKey": "USER-1",
              "menus": [
                { "menuCode": "BM-UNKNOWN-MENU", "count": 1 }
              ]
            }
        """.trimIndent()

        assertThatThrownBy {
            processor.processBaeminPayload(json)
        }.isInstanceOf(CoreException::class.java)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.MENU_MISMATCH_IN_ORDER)
    }
}
