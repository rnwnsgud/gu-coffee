package com.coffee.gu.stamp;

import com.coffee.gu.EventLogRepository;
import com.coffee.gu.Principal;
import com.coffee.gu.StampEarnEvent;
import com.coffee.gu.coupon.Coupon;
import com.coffee.gu.coupon.IssuedCoupon;
import com.coffee.gu.coupon.IssuedCouponRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

@Component
public class StampRewardManager {

    private final StampRewardPlanner stampRewardPlanner;
    private final StampRepository stampRepository;
    private final StampRecorder stampRecorder;
    private final IssuedCouponRepository issuedCouponRepository;
    private final EventLogRepository eventLogRepository;

    public StampRewardManager(StampRewardPlanner stampRewardPlanner, StampRepository stampRepository, IssuedCouponRepository issuedCouponRepository, EventLogRepository eventLogRepository, StampRecorder stampRecorder) {
        this.stampRewardPlanner = stampRewardPlanner;
        this.stampRepository = stampRepository;
        this.issuedCouponRepository = issuedCouponRepository;
        this.eventLogRepository = eventLogRepository;
        this.stampRecorder = stampRecorder;
    }

    public void reward(Principal principal, String orderKey, Long stampQuantity, LocalDateTime expiredAt) {
        if (stampQuantity <= 0) return;
        List<Stamp> stamps = LongStream.range(0, stampQuantity)
                .mapToObj(i -> Stamp.create(principal, orderKey, expiredAt))
                .toList();
        stampRepository.saveAll(stamps);
    }

    @Transactional
    public void stampToCouponWithIdempotency(StampEarnEvent event) {
        if (isDuplicated(event)) return;
        StampRewardPlan plan = stampRewardPlanner.plan(event);
        if (plan.isEmpty()) return;
        useStamps(plan);
        issueCoupons(plan);
        stampRecorder.recordStampCouponUsages(plan);
        stampRecorder.recordUse(event, plan.getCouponIssueCount());
        eventLogRepository.publish(event);
    }

    private boolean isDuplicated(StampEarnEvent event) {
        return !eventLogRepository.saveUniqueEvent(event);
    }

    private void useStamps(StampRewardPlan plan) {
        plan.getStampsToUse().forEach(Stamp::use);
        stampRepository.saveAll(plan.getStampsToUse());
    }

    private void issueCoupons(StampRewardPlan plan) {
        List<IssuedCoupon> issuedCoupons = LongStream.range(0, plan.getCouponIssueCount())
                .mapToObj(i -> IssuedCoupon.download(plan.getPrincipal(), Coupon.rewardCoupon()))
                .toList();
        issuedCoupons = issuedCouponRepository.saveAll(issuedCoupons);
        plan.assignIssuedCoupons(issuedCoupons);
    }





}
