package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import com.coffee.gu.menu.Menu
import org.springframework.stereotype.Service

@Service
class CouponService(
    private val couponFinder: CouponFinder,
    private val issuedCouponFinder: IssuedCouponFinder,
    private val couponManager: CouponManager
) {
    fun getCouponsForMenus(principal: Principal, menus: List<Menu>): List<Coupon> {
        val applicableCoupons = couponFinder.findApplicableCoupons(menus)
        val downloadedCoupons = couponFinder.findDownloadedCoupons(principal.key)
        val downloadedCouponIds = downloadedCoupons.map { it.id }.toSet()
        return applicableCoupons.filter { coupon -> !downloadedCouponIds.contains(coupon.id) }
    }

    fun download(principal: Principal, couponId: Long) {
        val coupon = couponFinder.getValidCoupon(couponId)
        val exist = issuedCouponFinder.existsByPrincipalKeyAndCouponId(principal, couponId)
        if (exist) throw CoreException(ErrorType.COUPON_ALREADY_DOWNLOADED, null)
        couponManager.issue(principal, coupon)
    }
}
