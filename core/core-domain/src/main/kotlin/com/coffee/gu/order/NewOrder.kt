package com.coffee.gu.order

import com.coffee.gu.Principal

class NewOrder(
    val principal: Principal,
    val storeId: Long,
    val lines: List<NewOrderLine>,
)
