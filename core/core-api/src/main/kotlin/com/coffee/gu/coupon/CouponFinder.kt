package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.CouponTargetType
import com.coffee.gu.menu.Menu
import com.coffee.gu.menu.MenuCategoryRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CouponFinder(
    private val couponRepository: CouponRepository,
    private val couponTargetRepository: CouponTargetRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
    private val menuCategoryRepository: MenuCategoryRepository
) {
    fun findApplicableCoupons(menus: List<Menu>): List<Coupon> {
        val menuIds = menus.map { it.id }
        val menuTargets = couponTargetRepository.findAllByTargetTypeAndTargetIdIn(
            CouponTargetType.MENU,
            menuIds
        )
        val categoryTargets = couponTargetRepository.findAllByTargetTypeAndTargetIdIn(
            CouponTargetType.MENU_CATEGORY,
            menuCategoryRepository.findAllByMenuIdIn(menuIds).map { it.categoryId }
        )
        val couponIds = (menuTargets.map { it.couponId } + categoryTargets.map { it.couponId }).distinct()
        return findAllByIdIn(couponIds)
    }

    fun findDownloadedCoupons(principalKey: String): List<Coupon> {
        val issuedCoupons = issuedCouponRepository.findAllByPrincipalKey(principalKey)
        if (issuedCoupons.isEmpty()) return emptyList()
        return couponRepository.findAllByIdIn(issuedCoupons.map { it.coupon.id })
    }

    fun findAllByIdIn(couponIds: List<Long>): List<Coupon> {
        return couponRepository.findAllByIdIn(couponIds)
    }

    fun getValidCoupon(couponId: Long): Coupon {
        return couponRepository.findById(couponId)
            .filter { coupon -> coupon.expiredAt.isAfter(LocalDateTime.now()) }
            .orElseThrow { CoreException(ErrorType.COUPON_NOT_FOUND_OR_EXPIRED, null) }
    }
}
