package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.Principal
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LimitedCouponIssueExecutor(
    private val limitedCouponFinder: LimitedCouponFinder,
    private val issuedCouponFinder: IssuedCouponFinder,
    private val limitedCouponManager: LimitedCouponManager,
) {
    @Transactional
    fun execute(principal: Principal, limitedCouponId: Long): IssuedCoupon {
        val limitedCoupon = limitedCouponFinder.getById(limitedCouponId)

        // 1. 1인 1매 중복 다운로드 검증
        val alreadyIssued = issuedCouponFinder.existsByPrincipalKeyAndCouponId(principal, limitedCoupon.coupon.id)
        if (alreadyIssued) {
            throw CoreException(ErrorType.COUPON_ALREADY_DOWNLOADED)
        }

        // 2. 선착순 재고 소진 검증 및 발급 수량 증가
        val updatedCoupon = limitedCoupon.issue()
        limitedCouponManager.save(updatedCoupon)

        // 3. 발급된 쿠폰함(IssuedCoupon)에 저장
        return limitedCouponManager.issue(principal, updatedCoupon)
    }
}
