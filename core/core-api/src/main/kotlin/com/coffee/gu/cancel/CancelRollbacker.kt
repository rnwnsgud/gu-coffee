package com.coffee.gu.cancel

import com.coffee.gu.Principal
import com.coffee.gu.coupon.IssuedCouponManager
import com.coffee.gu.menu.LimitedMenuStockManager
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderManager
import com.coffee.gu.payment.Payment
import com.coffee.gu.stamp.StampHandler
import org.springframework.stereotype.Component

@Component
class CancelRollbacker(
    private val orderManager: OrderManager,
    private val issuedCouponManager: IssuedCouponManager,
    private val stampHandler: StampHandler,
    private val limitedMenuStockManager: LimitedMenuStockManager,
) {
    fun rollback(order: Order, payment: Payment) {
        orderManager.cancel(order)
        limitedMenuStockManager.releaseIfLimited(
            orderKey = order.key,
            principalKey = order.principal.key,
            menuQuantities = order.toMenuQuantityMap(),
            restoreDbSoldQuantity = true,
        )
        if (payment.hasAppliedCoupon()) {
            issuedCouponManager.revert(Principal(payment.principal.key, payment.principal.type), payment.issuedCouponId!!)
        }
        stampHandler.revert(order)
    }
}
