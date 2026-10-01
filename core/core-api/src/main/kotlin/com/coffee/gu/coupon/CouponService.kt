package com.coffee.gu.coupon

import com.coffee.gu.Principal
import com.coffee.gu.lock.LockKeyGenerator
import com.coffee.gu.lock.LockManager
import com.coffee.gu.menu.Menu
import org.springframework.stereotype.Service
@Service
class CouponService(
    private val couponFinder: CouponFinder,
    private val couponDownloadExecutor: CouponDownloadExecutor,
    private val lockManager: LockManager,
) {
    fun getCouponsForMenus(principal: Principal, menus: List<Menu>): List<Coupon> {
        val applicableCoupons = couponFinder.findApplicableCoupons(menus)
        val downloadedCoupons = couponFinder.findDownloadedCoupons(principal.key)
        val downloadedCouponIds = downloadedCoupons.map { it.id }.toSet()
        return applicableCoupons.filter { coupon -> !downloadedCouponIds.contains(coupon.id) }
    }

    fun download(principal: Principal, couponId: Long) {
        val lockKey = LockKeyGenerator.generateCouponDownloadKey(couponId, principal.key)
        lockManager.executeWithLock(lockKey) {
            couponDownloadExecutor.download(principal, couponId)
        }
    }
}
