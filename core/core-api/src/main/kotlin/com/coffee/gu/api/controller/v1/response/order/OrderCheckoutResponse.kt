package com.coffee.gu.api.controller.v1.response.order

import com.coffee.gu.api.controller.v1.response.IssuedCouponResponse
import com.coffee.gu.coupon.IssuedCoupon
import com.coffee.gu.order.Order
import java.math.BigDecimal

class OrderCheckoutResponse(
    val key: String,
    val name: String,
    val totalPrice: BigDecimal,
    val lines: List<OrderLineResponse>,
    val usableCoupons: List<IssuedCouponResponse>,
) {
    companion object {
        @JvmStatic
        fun of(order: Order, issuedCoupons: List<IssuedCoupon>): OrderCheckoutResponse {
            return OrderCheckoutResponse(
                key = order.key,
                name = order.name,
                totalPrice = order.totalPrice,
                lines = order.lines.map { line ->
                    OrderLineResponse(
                        menuId = line.menuId,
                        menuName = line.menuName,
                        imageUrl = line.imageUrl ?: "",
                        description = line.description ?: "",
                        quantity = line.quantity,
                        unitPrice = line.unitPrice,
                        totalPrice = line.totalPrice
                    )
                },
                usableCoupons = IssuedCouponResponse.from(issuedCoupons)
            )
        }
    }
}
