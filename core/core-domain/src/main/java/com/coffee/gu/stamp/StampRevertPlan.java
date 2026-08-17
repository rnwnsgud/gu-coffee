package com.coffee.gu.stamp;

import com.coffee.gu.coupon.IssuedCoupon;

import java.util.List;

public class StampRevertPlan {

    private final List<Stamp> stampsToCancel;
    private final List<Stamp> usedStamps;
    private final List<IssuedCoupon> rewardCouponsToCancel;

    public StampRevertPlan(
            List<Stamp> stampsToCancel,
            List<Stamp> usedStamps,
            List<IssuedCoupon> rewardCouponsToCancel
    ) {
        this.stampsToCancel = stampsToCancel;
        this.usedStamps = usedStamps;
        this.rewardCouponsToCancel = rewardCouponsToCancel;
    }

    public static StampRevertPlan empty() {
        return new StampRevertPlan(List.of(), List.of(), List.of());
    }

    public boolean isEmpty() {
        return stampsToCancel.isEmpty();
    }

    public boolean hasRewardCouponsToCancel() {
        return !rewardCouponsToCancel.isEmpty();
    }

    public boolean hasUsedRewardCoupon() {
        return rewardCouponsToCancel.stream()
                .anyMatch(IssuedCoupon::isUsed);
    }

    public List<Stamp> getStampsToCancel() {
        return stampsToCancel;
    }

    public List<IssuedCoupon> getRewardCouponsToCancel() {
        return rewardCouponsToCancel;
    }

    public int cancelStampCount() {
        return stampsToCancel.size();
    }
}
