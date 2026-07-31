package com.coffee.gu.api.controller.v1;

import com.coffee.gu.Principal;
import com.coffee.gu.api.controller.v1.response.IssuedCouponResponse;
import com.coffee.gu.auth.Authenticated;
import com.coffee.gu.coupon.CouponService;
import com.coffee.gu.coupon.IssuedCoupon;
import com.coffee.gu.coupon.IssuedCouponService;

import com.coffee.gu.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CouponController {

    private final CouponService couponService;
    private final IssuedCouponService issuedCouponService;

    public CouponController(CouponService couponService, IssuedCouponService issuedCouponService) {
        this.couponService = couponService;
        this.issuedCouponService = issuedCouponService;
    }

    @GetMapping("/v1/issued-coupons")
    private ApiResponse<List<IssuedCouponResponse>> getIssuedCoupons(@Authenticated Principal principal) {
        List<IssuedCoupon> issuedCoupons = issuedCouponService.getIssuedCoupons(principal);
        return ApiResponse.success(IssuedCouponResponse.from(issuedCoupons));
    }

    @PostMapping("/v1/coupons/{couponId}/download")
    private ApiResponse<?> download(
            @Authenticated Principal principal,
            @PathVariable Long couponId) {
        couponService.download(principal, couponId);
        return ApiResponse.success();
    }

}
