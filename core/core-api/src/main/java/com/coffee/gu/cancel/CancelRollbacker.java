package com.coffee.gu.cancel;

import com.coffee.gu.Principal;
import com.coffee.gu.coupon.IssuedCouponManager;
import com.coffee.gu.order.Order;
import com.coffee.gu.order.OrderManager;
import com.coffee.gu.payment.Payment;
import com.coffee.gu.stamp.StampHandler;
import org.springframework.stereotype.Component;

@Component
public class CancelRollbacker {

    private final OrderManager orderManager;
    private final IssuedCouponManager issuedCouponManager;
    private final StampHandler stampHandler;

    public CancelRollbacker(OrderManager orderManager, IssuedCouponManager issuedCouponManager, StampHandler stampHandler) {
        this.orderManager = orderManager;
        this.issuedCouponManager = issuedCouponManager;
        this.stampHandler = stampHandler;
    }

    public void rollback(Order order, Payment payment) {
        orderManager.cancel(order);
        if (payment.hasAppliedCoupon()) issuedCouponManager.revert(new Principal(payment.getPrincipal().getKey(), payment.getPrincipal().getType()), payment.getIssuedCouponId());
        stampHandler.revert(order);
    }


}
