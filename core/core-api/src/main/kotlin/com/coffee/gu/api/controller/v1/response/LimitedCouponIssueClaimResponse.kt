package com.coffee.gu.api.controller.v1.response

data class LimitedCouponIssueClaimResponse(
    val status: String,
    val eventId: String,
    val message: String,
) {
    companion object {
        fun accepted(eventId: String): LimitedCouponIssueClaimResponse =
            LimitedCouponIssueClaimResponse(
                status = "ACCEPTED",
                eventId = eventId,
                message = "쿠폰 발급 요청이 성공적으로 접수되었습니다.",
            )
    }
}
