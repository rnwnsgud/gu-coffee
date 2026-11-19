package com.coffee.gu.delivery.webhook.controller.v1.request

import com.fasterxml.jackson.annotation.JsonProperty

data class BaeminOrderMenuPayload(
    @param:JsonProperty("menuCode") val menuCode: String = "",
    @param:JsonProperty("count") val count: Long = 0L,
)

data class BaeminOrderWebhookPayload(
    @param:JsonProperty("orderNo") val orderNo: String = "",
    @param:JsonProperty("shopNo") val shopNo: Long = 0L,
    @param:JsonProperty("memberKey") val memberKey: String = "",
    @param:JsonProperty("menus") val menus: List<BaeminOrderMenuPayload> = emptyList(),
)
