package com.coffee.gu.order

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderState
import io.hypersistence.tsid.TSID
import java.math.BigDecimal

class Order(
    val key: String,
    val name: String,
    val principal: Principal,
    val storeId: Long,
    val totalPrice: BigDecimal,
    var state: OrderState,
    var lines: List<OrderLine>,
) {
    fun fillOrderLines(lines: List<OrderLine>) {
        this.lines = lines
    }

    fun pay() {
        this.state = OrderState.PAID
    }

    fun paid() {
        pay()
    }

    fun cancel() {
        this.state = OrderState.CANCELED
    }

    fun canceled() {
        cancel()
    }

    fun validateOwner(principal: Principal) {
        if (this.principal != principal) throw CoreException(ErrorType.NOT_FOUND_DATA)
    }

    fun toMenuQuantityMap(): Map<Long, Int> =
        lines.groupBy { it.menuId }.mapValues { (_, items) -> items.sumOf { it.quantity }.toInt() }

    companion object {
        fun generateKey(): String = TSID.Factory.getTsid().toString()

        fun create(
            newOrder: NewOrder,
            orderMenus: OrderMenus,
            key: String = generateKey(),
        ): Order {
            if (newOrder.lines.isEmpty()) throw CoreException(ErrorType.NOT_FOUND_DATA)
            val orderLines = newOrder.lines.map { line ->
                val menu = orderMenus.getByMenuId(line.menuId)
                val salesPrice = menu.salesPrice
                OrderLine.create(
                    orderKey = key,
                    menuId = line.menuId,
                    menuName = menu.name,
                    imageUrl = menu.imageUrl ?: "",
                    description = menu.description ?: "",
                    quantity = line.quantity,
                    unitPrice = salesPrice,
                    totalPrice = salesPrice.multiply(BigDecimal.valueOf(line.quantity)),
                    isStampEligible = menu.isStampEligible,
                )
            }
            val totalPrice = orderLines.fold(BigDecimal.ZERO) { acc, line -> acc.add(line.totalPrice) }
            return Order(
                key = key,
                name = createOrderName(orderLines),
                principal = newOrder.principal,
                storeId = newOrder.storeId,
                totalPrice = totalPrice,
                state = OrderState.CREATED,
                lines = orderLines,
            )
        }

        private fun createOrderName(orderLines: List<OrderLine>): String {
            val firstMenuName = orderLines.first().menuName
            if (orderLines.size == 1) {
                return firstMenuName
            }
            return "$firstMenuName 외 ${orderLines.size - 1}개"
        }
    }
}