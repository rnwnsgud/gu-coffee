package com.coffee.gu.coupon;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.IssuedCouponState;

public class IssuedCoupon {
    private Long id;
    private Principal principal;
    private IssuedCouponState state;
    private Coupon coupon;

    public IssuedCoupon(Long id, Principal principal, IssuedCouponState state, Coupon coupon) {
        this.id = id;
        this.principal = principal;
        this.state = state;
        this.coupon = coupon;
    }

    public static IssuedCoupon download(Principal principal, Coupon coupon) {
        return new IssuedCoupon(null, principal, IssuedCouponState.DOWNLOADED, coupon);
    }

    public Long getId() {
        return id;
    }

    public IssuedCouponState getState() {
        return state;
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public void use() {
        this.state = IssuedCouponState.USED;
    }

    public void revert() {
        this.state = IssuedCouponState.DOWNLOADED;
    }

    public void cancel() {
        this.state = IssuedCouponState.CANCELED;
    }

    public boolean isUsed() {
        return this.state == IssuedCouponState.USED;
    }

    public Principal getPrincipal() {
        return principal;
    }
}
