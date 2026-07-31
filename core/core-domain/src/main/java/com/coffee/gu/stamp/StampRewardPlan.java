package com.coffee.gu.stamp;

import com.coffee.gu.Principal;
import com.coffee.gu.coupon.Coupon;
import com.coffee.gu.coupon.IssuedCoupon;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StampRewardPlan {

    private final Principal principal;
    private final long couponIssueCount;
    private final long stampUseCount;
    private final List<Stamp> stampsToUse;
    private List<IssuedCoupon> issuedCoupons;

    public StampRewardPlan(
            Principal principal,
            long couponIssueCount,
            long stampUseCount,
            List<Stamp> stampsToUse
    ) {
        this.principal = principal;
        this.couponIssueCount = couponIssueCount;
        this.stampUseCount = stampUseCount;
        this.stampsToUse = stampsToUse;
        this.issuedCoupons = List.of();
    }

    public static StampRewardPlan empty(Principal principal) {
        return new StampRewardPlan(principal, 0, 0, List.of());
    }

    public boolean isEmpty() {
        return couponIssueCount <= 0 || stampsToUse.isEmpty();
    }

    public void assignIssuedCoupons(List<IssuedCoupon> issuedCoupons) {
        this.issuedCoupons = issuedCoupons;
    }

    public List<StampCouponUsage> createStampCouponUsages(
            List<IssuedCoupon> issuedCoupons,
            List<Stamp> stamps
    ) {
        List<StampCouponUsage> usages = new ArrayList<>();
        int stampCountPerCoupon = Coupon.REWARD_COUPON_STAMP_COUNT;

        for (int i = 0; i < issuedCoupons.size(); i++) {
            IssuedCoupon issuedCoupon = issuedCoupons.get(i);

            int fromIndex = i * stampCountPerCoupon;
            int toIndex = fromIndex + stampCountPerCoupon;

            List<Stamp> couponStamps = stamps.subList(fromIndex, toIndex);
            LocalDateTime now = LocalDateTime.now();

            for (Stamp stamp : couponStamps) {
                usages.add(StampCouponUsage.create(stamp.getId(), issuedCoupon.getId(), now));
            }
        }

        return usages;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public long getCouponIssueCount() {
        return couponIssueCount;
    }

    public long getStampUseCount() {
        return stampUseCount;
    }

    public List<Stamp> getStampsToUse() {
        return stampsToUse;
    }

    public List<IssuedCoupon> getIssuedCoupons() {
        return issuedCoupons;
    }
}
