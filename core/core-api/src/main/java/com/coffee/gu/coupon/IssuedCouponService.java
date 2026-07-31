package com.coffee.gu.coupon;

import com.coffee.gu.Principal;
import com.coffee.gu.menu.Menu;
import com.coffee.gu.menu.MenuFinder;
import com.coffee.gu.order.Order;
import com.coffee.gu.order.OrderLine;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IssuedCouponService {

    private final IssuedCouponFinder issuedCouponFinder;
    private final CouponFinder couponFinder;
    private final MenuFinder menuFinder;

    public IssuedCouponService(IssuedCouponFinder issuedCouponFinder, CouponFinder couponFinder, MenuFinder menuFinder) {
        this.issuedCouponFinder = issuedCouponFinder;
        this.couponFinder = couponFinder;
        this.menuFinder = menuFinder;
    }

    public List<IssuedCoupon> getIssuedCoupons(Principal principal) {
        return issuedCouponFinder.findAllByPrincipalKey(principal.getKey());
    }

    public List<IssuedCoupon> getIssuedCouponsForCheckout(Principal principal, Order order) {
        if (order.getLines().isEmpty()) return List.of();
        List<Menu> menus = menuFinder.findAllByIdIn(order.getLines().stream().map(OrderLine::getMenuId).toList());
        List<Coupon> applicableCoupons = couponFinder.findApplicableCoupons(menus);
        if (applicableCoupons.isEmpty()) return List.of();
        return issuedCouponFinder.getUsableAllByPrincipalAndCoupons(principal, applicableCoupons);
    }
}
