package com.coffee.gu.order

import com.coffee.gu.Principal

data class NewOrder(
    val principal: Principal,
    val storeId: Long,
    val lines: List<NewOrderLine>,
) {
    val menuIds: Set<Long>
        get() = lines.map { it.menuId }.toSet()

    fun toMenuQuantityMap(): Map<Long, Int> =
        lines.groupBy { it.menuId }.mapValues { (_, items) -> items.sumOf { it.quantity }.toInt() }
}