package com.coffee.gu.api.controller.v1

import com.coffee.gu.Principal
import com.coffee.gu.api.controller.v1.response.stamp.StampHistoryResponse
import com.coffee.gu.api.controller.v1.response.stamp.StampResponse
import com.coffee.gu.auth.Authenticated
import com.coffee.gu.response.ApiResponse
import com.coffee.gu.stamp.StampService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class StampController(
    private val stampService: StampService,
) {
    @GetMapping("/v1/stamps")
    fun getStamps(@Authenticated principal: Principal): ApiResponse<StampResponse> {
        val stampCount = stampService.count(principal)
        return ApiResponse.success(StampResponse(stampCount.count))
    }

    @GetMapping("/v1/stamps/history")
    fun getStampHistories(@Authenticated principal: Principal): ApiResponse<StampHistoryResponse> {
        val stampExpiringSoonCount = stampService.countExpiringSoon(principal)
        val stampHistories = stampService.getStampHistories(principal)
        return ApiResponse.success(StampHistoryResponse.of(stampExpiringSoonCount.count, stampHistories))
    }
}
