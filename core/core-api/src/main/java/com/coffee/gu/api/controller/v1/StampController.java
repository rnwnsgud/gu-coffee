package com.coffee.gu.api.controller.v1;

import com.coffee.gu.Principal;
import com.coffee.gu.api.controller.v1.response.stamp.StampHistoryResponse;
import com.coffee.gu.api.controller.v1.response.stamp.StampResponse;
import com.coffee.gu.auth.Authenticated;
import com.coffee.gu.response.ApiResponse;
import com.coffee.gu.stamp.StampCount;
import com.coffee.gu.stamp.StampExpiringSoonCount;
import com.coffee.gu.stamp.StampHistory;
import com.coffee.gu.stamp.StampService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class StampController {

    private final StampService stampService;

    public StampController(StampService stampService) {
        this.stampService = stampService;
    }

    @GetMapping("/v1/stamps")
    public ApiResponse<StampResponse> getStamps(@Authenticated Principal principal) {
        StampCount stampCount = stampService.count(principal);
        return ApiResponse.success(new StampResponse(stampCount.count()));
    }

    @GetMapping("/v1/stamps/history")
    public ApiResponse<StampHistoryResponse> getStampHistories(@Authenticated Principal principal) {
        StampExpiringSoonCount stampExpiringSoonCount = stampService.countExpiringSoon(principal);
        List<StampHistory> stampHistories = stampService.getStampHistories(principal);
        return ApiResponse.success(StampHistoryResponse.of(stampExpiringSoonCount.count(), stampHistories));
    }
}
