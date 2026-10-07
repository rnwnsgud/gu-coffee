package com.coffee.gu.api.controller.v1

import com.coffee.gu.Principal
import com.coffee.gu.api.controller.v1.response.IssuedCouponResponse
import com.coffee.gu.api.controller.v1.response.LimitedCouponResponse
import com.coffee.gu.auth.Authenticated
import com.coffee.gu.coupon.LimitedCouponService
import com.coffee.gu.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LimitedCouponController(
    private val limitedCouponService: LimitedCouponService,
) {

    @GetMapping("/v1/limited-coupons/{limitedCouponId}")
    fun get(@PathVariable limitedCouponId: Long): ApiResponse<LimitedCouponResponse> {
        val limitedCoupon = limitedCouponService.get(limitedCouponId)
        return ApiResponse.success(LimitedCouponResponse.from(limitedCoupon))
    }

    @PostMapping("/v1/limited-coupons/{limitedCouponId}/issue")
    fun issue(
        @Authenticated principal: Principal,
        @PathVariable limitedCouponId: Long,
    ): ApiResponse<IssuedCouponResponse> {
        val issuedCoupon = limitedCouponService.issue(principal, limitedCouponId)
        return ApiResponse.success(IssuedCouponResponse.from(issuedCoupon))
    }
}
