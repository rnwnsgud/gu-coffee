package com.coffee.gu.api.controller.v1;

import com.coffee.gu.Principal;
import com.coffee.gu.api.controller.v1.request.CancelRequest;
import com.coffee.gu.cancel.CancelService;
import com.coffee.gu.order.Order;
import com.coffee.gu.order.OrderService;
import com.coffee.gu.enums.OrderState;
import com.coffee.gu.response.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CancelController {

    private final CancelService cancelService;
    private final OrderService orderService;

    public CancelController(CancelService cancelService, OrderService orderService) {
        this.cancelService = cancelService;
        this.orderService = orderService;
    }

    @PostMapping("/v1/cancel")
    public ApiResponse<?> cancelOrder(Principal principal, CancelRequest request) {
        Order order = orderService.getOrder(request.orderKey(), OrderState.PAID);
        cancelService.cancel(order, principal);
        return ApiResponse.success();
    }
}
