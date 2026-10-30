package com.coffee.gu.coupon.messaging

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.LimitedCouponIssueEvent
import com.coffee.gu.coupon.LimitedCouponIssueExecutor
import com.coffee.gu.kafka.KafkaTopic
import com.coffee.gu.lock.LimitedCouponClaimer
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class LimitedCouponIssueConsumer(
    private val limitedCouponIssueExecutor: LimitedCouponIssueExecutor,
    private val limitedCouponClaimer: LimitedCouponClaimer,
) {
    companion object {
        private val log = LoggerFactory.getLogger(LimitedCouponIssueConsumer::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopic.LIMITED_COUPON_ISSUE_V1],
        groupId = "coffee-coupon-group",
    )
    fun consume(event: LimitedCouponIssueEvent) {
        log.info(
            "LimitedCouponIssueEvent 수신: eventId={}, principalKey={}, limitedCouponId={}",
            event.eventId,
            event.principal.key,
            event.limitedCouponId,
        )
        try {
            limitedCouponIssueExecutor.execute(event.principal, event.limitedCouponId)
            log.info(
                "LimitedCouponIssueEvent 발급 완료: eventId={}, principalKey={}, limitedCouponId={}",
                event.eventId,
                event.principal.key,
                event.limitedCouponId,
            )
        } catch (e: CoreException) {
            if (e.errorType == ErrorType.COUPON_ALREADY_DOWNLOADED) {
                log.warn(
                    "이미 발급된 쿠폰입니다. 멱등 차단: eventId={}, principalKey={}, limitedCouponId={}",
                    event.eventId,
                    event.principal.key,
                    event.limitedCouponId,
                )
            } else {
                log.error(
                    "선착순 쿠폰 발급 실패 (재고 롤백 수행): eventId={}, principalKey={}, limitedCouponId={}, error={}",
                    event.eventId,
                    event.principal.key,
                    event.limitedCouponId,
                    e.errorType,
                    e,
                )
                limitedCouponClaimer.rollbackClaim(event.limitedCouponId, event.principal.key)
                throw e
            }
        } catch (e: Exception) {
            log.error(
                "선착순 쿠폰 발급 중 예기치 않은 오류 발생: eventId={}, principalKey={}, limitedCouponId={}",
                event.eventId,
                event.principal.key,
                event.limitedCouponId,
                e,
            )
            throw e
        }
    }
}
