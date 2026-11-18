package com.coffee.gu.webhook.order

import com.coffee.gu.Principal
import com.coffee.gu.enums.DeliveryPlatform
import com.coffee.gu.enums.OrderChannel
import com.coffee.gu.order.ExternalOrderCreator
import com.coffee.gu.order.NewOrder
import com.coffee.gu.order.NewOrderLine
import com.coffee.gu.webhook.WebhookIdempotencyRecord
import com.coffee.gu.webhook.WebhookIdempotencyRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mockito.mock
import org.mockito.kotlin.any

class WebhookIdempotencyManagerTest {

    private val webhookIdempotencyRepository: WebhookIdempotencyRepository = mock()
    private val externalOrderCreator: ExternalOrderCreator = mock()
    private val manager = WebhookIdempotencyManager(webhookIdempotencyRepository, externalOrderCreator)

    @Test
    @DisplayName("이미 처리된 멱등성 키가 존재하면 주문을 새로 생성하지 않고 기존 orderKey를 반환한다")
    fun returnExistingOrderKeyWithoutCreatingDuplicateOrder() {
        val existingRecord = WebhookIdempotencyRecord(
            id = 10L,
            platform = DeliveryPlatform.BAEMIN,
            idempotencyKey = "IDEMP-BM-1",
            orderKey = "EXISTING-ORDER-KEY",
        )
        given(
            webhookIdempotencyRepository.findByPlatformAndIdempotencyKey(DeliveryPlatform.BAEMIN, "IDEMP-BM-1"),
        ).willReturn(existingRecord)

        val newOrder = NewOrder(
            principal = Principal.guest("BAEMIN:U1"),
            storeId = 1L,
            lines = listOf(NewOrderLine(menuId = 1L, quantity = 1L)),
            channel = OrderChannel.DELIVERY_PARTNER,
        )

        val result = manager.createOrderIdempotently(DeliveryPlatform.BAEMIN, "IDEMP-BM-1", newOrder)

        assertThat(result).isEqualTo("EXISTING-ORDER-KEY")
        then(externalOrderCreator).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("신규 멱등성 키 요청이면 주문을 생성하고 멱등성 레코드를 저장한 뒤 orderKey를 반환한다")
    fun createNewOrderAndSaveIdempotencyRecord() {
        val newOrder = NewOrder(
            principal = Principal.guest("COUPANG_EATS:U2"),
            storeId = 1L,
            lines = listOf(NewOrderLine(menuId = 2L, quantity = 1L)),
            channel = OrderChannel.DELIVERY_PARTNER,
        )
        given(
            webhookIdempotencyRepository.findByPlatformAndIdempotencyKey(DeliveryPlatform.COUPANG_EATS, "IDEMP-CE-1"),
        ).willReturn(null)
        given(externalOrderCreator.create(newOrder)).willReturn("NEW-ORDER-KEY")
        given(webhookIdempotencyRepository.save(any())).willAnswer { it.getArgument(0) }

        val result = manager.createOrderIdempotently(DeliveryPlatform.COUPANG_EATS, "IDEMP-CE-1", newOrder)

        assertThat(result).isEqualTo("NEW-ORDER-KEY")
    }
}
