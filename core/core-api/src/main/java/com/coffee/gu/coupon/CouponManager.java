package com.coffee.gu.coupon;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.Principal;
import com.coffee.gu.enums.IssuedCouponState;
import com.coffee.gu.enums.PrincipalType;

import org.springframework.stereotype.Component;

@Component
public class CouponManager {

    private final IssuedCouponRepository issuedCouponRepository;

    public CouponManager(IssuedCouponRepository issuedCouponRepository) {
        this.issuedCouponRepository = issuedCouponRepository;
    }

    public void issue(Principal principal, Coupon coupon) {
        if (principal.getType() == PrincipalType.GUEST) throw new CoreException(ErrorType.UNAUTHORIZED, null);
        issuedCouponRepository.save(IssuedCoupon.download(principal, coupon));
    }
}
