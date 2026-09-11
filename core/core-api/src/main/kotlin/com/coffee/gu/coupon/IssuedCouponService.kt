package com.coffee.gu.coupon

import com.coffee.gu.Principal
import com.coffee.gu.menu.MenuFinder
import com.coffee.gu.order.Order
import org.springframework.stereotype.Service

@Service
class IssuedCouponService(
    private val issuedCouponFinder: IssuedCouponFinder,
    private val couponFinder: CouponFinder,
    private val menuFinder: MenuFinder
) {
    fun getIssuedCoupons(principal: Principal): List<IssuedCoupon> {
        return issuedCouponFinder.findAllByPrincipalKey(principal.key)
    }

    fun getIssuedCouponsForCheckout(principal: Principal, order: Order): List<IssuedCoupon> {
        if (order.lines.isEmpty()) return emptyList()
        val menus = menuFinder.findAllByIdIn(order.lines.map { it.menuId })
        val applicableCoupons = couponFinder.findApplicableCoupons(menus)
        if (applicableCoupons.isEmpty()) return emptyList()
        return issuedCouponFinder.getUsableAllByPrincipalAndCoupons(principal, applicableCoupons)
    }
}
