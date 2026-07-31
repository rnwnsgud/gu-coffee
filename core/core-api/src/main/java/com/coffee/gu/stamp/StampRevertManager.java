package com.coffee.gu.stamp;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.coupon.IssuedCoupon;
import com.coffee.gu.coupon.IssuedCouponRepository;
import com.coffee.gu.order.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StampRevertManager {

    private final StampRevertPlanner stampRevertPlanner;
    private final StampRepository stampRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final StampRecorder stampRecorder;

    public StampRevertManager(StampRevertPlanner stampRevertPlanner, StampRepository stampRepository, IssuedCouponRepository issuedCouponRepository, StampRecorder stampRecorder) {
        this.stampRevertPlanner = stampRevertPlanner;
        this.stampRepository = stampRepository;
        this.issuedCouponRepository = issuedCouponRepository;
        this.stampRecorder = stampRecorder;

    }

    public void validateRevertable(Order order) {
        StampRevertPlan plan = stampRevertPlanner.plan(order);
        validate(plan);
    }

    @Transactional
    public void revert(Order order) {
        StampRevertPlan plan = stampRevertPlanner.plan(order);
        if (plan.isEmpty()) return;
        validate(plan);
        cancelRewardCoupons(plan);
        cancelStamps(plan);
        stampRecorder.recordCancel(order, plan);
    }

    private void validate(StampRevertPlan plan) {
        if (plan.hasUsedRewardCoupon()) {
            throw new CoreException(
                    ErrorType.INVALID_REQUEST,
                    "이미 사용된 리워드 쿠폰이 있어 주문을 취소할 수 없습니다."
            );
        }
    }

    private void cancelRewardCoupons(StampRevertPlan plan) {
        if (!plan.hasRewardCouponsToCancel()) return;
        plan.getRewardCouponsToCancel().forEach(IssuedCoupon::cancel);
        issuedCouponRepository.saveAll(plan.getRewardCouponsToCancel());
    }

    private void cancelStamps(StampRevertPlan plan) {
        plan.getStampsToCancel().forEach(Stamp::cancel);
        stampRepository.saveAll(plan.getStampsToCancel());
    }


}
