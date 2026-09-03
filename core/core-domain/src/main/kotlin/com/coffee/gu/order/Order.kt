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

    fun cancel() {
        this.state = OrderState.CANCELED
    }

    fun validateOwner(principal: Principal) {
        if (this.principal != principal) throw CoreException(ErrorType.NOT_FOUND_DATA)
    }

    companion object {
        fun create(name: String, principal: Principal, storeId: Long, totalPrice: BigDecimal): Order {
            return Order(
                key = TSID.Factory.getTsid().toString(),
                name = name,
                principal = principal,
                storeId = storeId,
                totalPrice = totalPrice,
                state = OrderState.CREATED,
                lines = emptyList()
            )
        }
    }
}