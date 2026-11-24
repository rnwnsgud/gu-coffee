package com.coffee.gu.delivery.webhook.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.delivery.webhook.controller.v1.request.BaeminOrderWebhookPayload
import com.coffee.gu.delivery.webhook.controller.v1.request.CoupangEatsOrderWebhookPayload
import com.coffee.gu.enums.DeliveryPlatform
import com.coffee.gu.enums.OrderChannel
import com.coffee.gu.order.NewOrder
import com.coffee.gu.order.NewOrderLine
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class DeliveryWebhookPayloadProcessor(
    private val externalMenuMappingFinder: ExternalMenuMappingFinder,
    private val jsonMapper: JsonMapper,
) {

    fun processBaeminPayload(rawPayload: String): NewOrder {
        val payload = try {
            jsonMapper.readValue(rawPayload, BaeminOrderWebhookPayload::class.java)
        } catch (_: Exception) {
            throw CoreException(ErrorType.INVALID_REQUEST, "배달의민족 웹훅 JSON 형식이 올바르지 않습니다.")
        }

        if (payload.shopNo <= 0 || payload.memberKey.isBlank() || payload.menus.isEmpty() ||
            payload.menus.any { it.menuCode.isBlank() || it.count <= 0 }
        ) {
            throw CoreException(ErrorType.INVALID_REQUEST, "유효하지 않은 배달의민족 주문 페이로드입니다.")
        }

        val menuIdByCode = externalMenuMappingFinder.resolveMenuIds(
            platform = DeliveryPlatform.BAEMIN,
            externalMenuCodes = payload.menus.map { it.menuCode },
        )

        return NewOrder(
            principal = Principal.guest("BAEMIN:${payload.memberKey}"),
            storeId = payload.shopNo,
            lines = payload.menus.map { item ->
                NewOrderLine(
                    menuId = menuIdByCode.getValue(item.menuCode.trim()),
                    quantity = item.count,
                )
            },
            channel = OrderChannel.DELIVERY_PARTNER,
        )
    }

    fun processCoupangEatsPayload(rawPayload: String): NewOrder {
        val payload = try {
            jsonMapper.readValue(rawPayload, CoupangEatsOrderWebhookPayload::class.java)
        } catch (_: Exception) {
            throw CoreException(ErrorType.INVALID_REQUEST, "쿠팡이츠 웹훅 JSON 형식이 올바르지 않습니다.")
        }

        if (payload.merchantStoreId <= 0 || payload.buyerId.isBlank() || payload.orderItems.isEmpty() ||
            payload.orderItems.any { it.productCode.isBlank() || it.qty <= 0 }
        ) {
            throw CoreException(ErrorType.INVALID_REQUEST, "유효하지 않은 쿠팡이츠 주문 페이로드입니다.")
        }

        val menuIdByCode = externalMenuMappingFinder.resolveMenuIds(
            platform = DeliveryPlatform.COUPANG_EATS,
            externalMenuCodes = payload.orderItems.map { it.productCode },
        )

        return NewOrder(
            principal = Principal.guest("COUPANG_EATS:${payload.buyerId}"),
            storeId = payload.merchantStoreId,
            lines = payload.orderItems.map { item ->
                NewOrderLine(
                    menuId = menuIdByCode.getValue(item.productCode.trim()),
                    quantity = item.qty,
                )
            },
            channel = OrderChannel.DELIVERY_PARTNER,
        )
    }
}
