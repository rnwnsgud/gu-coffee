package com.coffee.gu.stamp;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.coupon.IssuedCoupon;
import com.coffee.gu.coupon.IssuedCouponRepository;
import com.coffee.gu.enums.StampState;
import com.coffee.gu.order.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StampRevertPlanner {

    private final StampRepository stampRepository;
    private final StampCouponUsageRepository stampCouponUsageRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    public StampRevertPlanner(StampRepository stampRepository, StampCouponUsageRepository stampCouponUsageRepository, IssuedCouponRepository issuedCouponRepository) {
        this.stampRepository = stampRepository;
        this.stampCouponUsageRepository = stampCouponUsageRepository;
        this.issuedCouponRepository = issuedCouponRepository;
    }

    public StampRevertPlan plan(Order order) {
        List<Stamp> stamps = stampRepository.findByOrderKey(order.getKey());
        List<Stamp> stampsToCancel = stamps.stream()
                .filter(stamp -> stamp.getState() != StampState.CANCELED)
                .toList();
        if (stampsToCancel.isEmpty()) {
            return StampRevertPlan.empty();
        }
        List<Stamp> usedStamps = stampsToCancel.stream()
                .filter(stamp -> stamp.getState() == StampState.USED)
                .toList();
        List<IssuedCoupon> rewardCouponsToCancel = findRewardCouponsIssuedBy(usedStamps);
        return new StampRevertPlan(
                stampsToCancel,
                usedStamps,
                rewardCouponsToCancel
        );
    }

    private List<IssuedCoupon> findRewardCouponsIssuedBy(List<Stamp> usedStamps) {
        List<Long> usedStampIds = usedStamps.stream()
                .map(Stamp::getId)
                .toList();

        List<StampCouponUsage> usages = stampCouponUsageRepository.findAllByStampIdIn(usedStampIds);

        if (usages.isEmpty()) {
            throw new CoreException(ErrorType.NOT_FOUND_DATA, null);
        }

        List<Long> issuedCouponIds = usages.stream()
                .map(StampCouponUsage::getIssuedCouponId)
                .distinct()
                .toList();

        List<IssuedCoupon> issuedCoupons = issuedCouponRepository.findAllByIdIn(issuedCouponIds);

        if (issuedCoupons.size() != issuedCouponIds.size()) {
            throw new CoreException(ErrorType.NOT_FOUND_DATA, null);
        }

        return issuedCoupons;
    }
}
