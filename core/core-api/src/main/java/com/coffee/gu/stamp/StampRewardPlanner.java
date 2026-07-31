package com.coffee.gu.stamp;

import com.coffee.gu.StampEarnEvent;
import com.coffee.gu.coupon.Coupon;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class StampRewardPlanner {

    private final StampRepository stampRepository;

    public StampRewardPlanner(StampRepository stampRepository) {
        this.stampRepository = stampRepository;
    }

    public StampRewardPlan plan(StampEarnEvent event) {
        Long availableCount = stampRepository.countAvailableStamps(
                event.getPrincipal().getKey(),
                LocalDateTime.now()
        );

        long couponIssueCount = availableCount / Coupon.REWARD_COUPON_STAMP_COUNT;

        if (couponIssueCount <= 0) {
            return StampRewardPlan.empty(event.getPrincipal());
        }

        long stampUseCount = couponIssueCount * Coupon.REWARD_COUPON_STAMP_COUNT;

        // todo 동시성까지 엄밀하게 보려면 나중에는 아래를 고려
        //1. 사용할 스탬프 조회 시 락
        //2. USED 상태 변경 조건부 업데이트
        //3. 이벤트 중복 처리 키 강화
        // 지금 단계에서는 eventLogRepository.saveUniqueEvent(event)로 이벤트 중복은 막고 있으니 우선 충분
        List<Stamp> stampsToUse = stampRepository.getAvailableStamps(
                event.getPrincipal().getKey(),
                LocalDateTime.now(),
                0,
                (int) stampUseCount
        );

        return new StampRewardPlan(
                event.getPrincipal(),
                couponIssueCount,
                stampUseCount,
                stampsToUse
        );
    }
}
