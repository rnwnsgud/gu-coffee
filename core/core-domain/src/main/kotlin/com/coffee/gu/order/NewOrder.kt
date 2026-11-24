package com.coffee.gu.order

import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderChannel

data class NewOrder(
    val principal: Principal,
    val storeId: Long,
    val lines: List<NewOrderLine>,
    val channel: OrderChannel = OrderChannel.APP,
) {
    val menuIds: Set<Long>
        get() = lines.map { it.menuId }.toSet()

    fun toMenuQuantityMap(): Map<Long, Int> =
        lines.groupBy { it.menuId }.mapValues { (_, items) -> items.sumOf { it.quantity }.toInt() }
}