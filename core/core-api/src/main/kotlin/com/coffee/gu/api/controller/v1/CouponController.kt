package com.coffee.gu.api.controller.v1

import com.coffee.gu.Principal
import com.coffee.gu.api.controller.v1.response.IssuedCouponResponse
import com.coffee.gu.auth.Authenticated
import com.coffee.gu.coupon.CouponService
import com.coffee.gu.coupon.IssuedCouponService
import com.coffee.gu.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CouponController(
    private val couponService: CouponService,
    private val issuedCouponService: IssuedCouponService,
) {
    @GetMapping("/v1/issued-coupons")
    fun getIssuedCoupons(@Authenticated principal: Principal): ApiResponse<List<IssuedCouponResponse>> {
        val issuedCoupons = issuedCouponService.getIssuedCoupons(principal)
        return ApiResponse.success(IssuedCouponResponse.from(issuedCoupons))
    }

    @PostMapping("/v1/coupons/{couponId}/download")
    fun download(
        @Authenticated principal: Principal,
        @PathVariable couponId: Long,
    ): ApiResponse<Unit> {
        couponService.download(principal, couponId)
        return ApiResponse.success()
    }
}
