package com.coffee.gu.coupon;


import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.Principal;
import com.coffee.gu.menu.Menu;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CouponService {

    private final CouponFinder couponFinder;
    private final IssuedCouponFinder issuedCouponFinder;
    private final CouponManager couponManager;

    public CouponService(CouponFinder couponFinder, IssuedCouponFinder issuedCouponFinder, CouponManager couponManager) {
        this.couponFinder = couponFinder;
        this.issuedCouponFinder = issuedCouponFinder;
        this.couponManager = couponManager;
    }

    public List<Coupon> getCouponsForMenus(Principal principal, List<Menu> menus) {
        List<Coupon> applicableCoupons = couponFinder.findApplicableCoupons(menus);
        List<Coupon> downloadedCoupons = couponFinder.findDownloadedCoupons(principal.getKey());
        Set<Long> downloadedCouponIds = downloadedCoupons.stream()
                .map(Coupon::getId)
                .collect(Collectors.toSet());
        return applicableCoupons.stream()
                .filter(coupon -> !downloadedCouponIds.contains(coupon.getId()))
                .collect(Collectors.toList());
    }

    public void download(Principal principal, Long couponId) {
        Coupon coupon = couponFinder.getValidCoupon(couponId);
        boolean exist = issuedCouponFinder.existsByPrincipalKeyAndCouponId(principal, couponId);
        if (exist) throw new CoreException(ErrorType.COUPON_ALREADY_DOWNLOADED, null);
        couponManager.issue(principal, coupon);
    }
}
