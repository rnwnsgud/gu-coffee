package com.coffee.gu.delivery.webhook.order

import com.coffee.gu.Principal
import com.coffee.gu.delivery.DeliveryWebhookIdempotencyRecord
import com.coffee.gu.delivery.DeliveryWebhookIdempotencyRepository
import com.coffee.gu.enums.DeliveryPlatform
import com.coffee.gu.enums.OrderChannel
import com.coffee.gu.order.ExternalOrderCreator
import com.coffee.gu.order.NewOrder
import com.coffee.gu.order.NewOrderLine
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

class DeliveryWebhookIdempotencyManagerTest {

    private val deliveryWebhookIdempotencyRepository: DeliveryWebhookIdempotencyRepository = mock()
    private val externalOrderCreator: ExternalOrderCreator = mock()
    private val manager = DeliveryWebhookIdempotencyManager(deliveryWebhookIdempotencyRepository, externalOrderCreator)

    @Test
    @DisplayName("이미 처리된 멱등성 키가 존재하면 주문을 새로 생성하지 않고 기존 orderKey를 반환한다")
    fun returnExistingOrderKeyWithoutCreatingDuplicateOrder() {
        val existingRecord = DeliveryWebhookIdempotencyRecord(
            id = 10L,
            platform = DeliveryPlatform.BAEMIN,
            idempotencyKey = "IDEMP-BM-1",
            orderKey = "EXISTING-ORDER-KEY",
        )
        given(
            deliveryWebhookIdempotencyRepository.findByPlatformAndIdempotencyKey(DeliveryPlatform.BAEMIN, "IDEMP-BM-1"),
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
            deliveryWebhookIdempotencyRepository.findByPlatformAndIdempotencyKey(DeliveryPlatform.COUPANG_EATS, "IDEMP-CE-1"),
        ).willReturn(null)
        given(externalOrderCreator.create(newOrder)).willReturn("NEW-ORDER-KEY")
        given(deliveryWebhookIdempotencyRepository.save(any())).willAnswer { it.getArgument(0) }

        val result = manager.createOrderIdempotently(DeliveryPlatform.COUPANG_EATS, "IDEMP-CE-1", newOrder)

        assertThat(result).isEqualTo("NEW-ORDER-KEY")
    }
}
