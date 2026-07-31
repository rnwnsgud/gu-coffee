package com.coffee.gu.coupon;


import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.Principal;
import com.coffee.gu.payment.Payment;
import org.springframework.stereotype.Component;

@Component
public class IssuedCouponManager {

    private final IssuedCouponFinder issuedCouponFinder;
    private final IssuedCouponRepository issuedCouponRepository;

    public IssuedCouponManager(IssuedCouponRepository issuedCouponRepository, IssuedCouponFinder issuedCouponFinder) {
        this.issuedCouponRepository = issuedCouponRepository;
        this.issuedCouponFinder = issuedCouponFinder;
    }

    public void use(Payment payment) {
        if (!payment.hasAppliedCoupon()) return;
        IssuedCoupon issuedCoupon = issuedCouponFinder.getById(payment.getIssuedCouponId());
        if (!issuedCoupon.getPrincipal().equals(payment.getPrincipal())) throw new CoreException(ErrorType.UNAUTHORIZED, null);
        issuedCoupon.use();
        issuedCouponRepository.save(issuedCoupon);
    }

    public void revert(Principal principal, Long issuedCouponId) {
        IssuedCoupon issuedCoupon = issuedCouponFinder.getById(issuedCouponId);
        if (!issuedCoupon.getPrincipal().equals(principal)) throw new CoreException(ErrorType.UNAUTHORIZED, null);
        issuedCoupon.revert();
        issuedCouponRepository.save(issuedCoupon);
    }
}
