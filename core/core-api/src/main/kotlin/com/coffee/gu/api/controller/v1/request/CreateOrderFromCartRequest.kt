package com.coffee.gu.api.controller.v1.request

class CreateOrderFromCartRequest(
    val storeId: Long,
    val cartItemIds: Set<Long>,
)
