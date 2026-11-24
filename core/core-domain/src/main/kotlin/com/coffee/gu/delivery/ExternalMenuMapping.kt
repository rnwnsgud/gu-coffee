package com.coffee.gu.delivery

import com.coffee.gu.enums.DeliveryPlatform

data class ExternalMenuMapping(
    val id: Long = 0,
    val platform: DeliveryPlatform,
    val externalMenuCode: String,
    val menuId: Long,
)
