package com.coffee.gu.coupon;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.Principal;
import com.coffee.gu.enums.IssuedCouponState;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class IssuedCouponFinder {

    private final IssuedCouponRepository issuedCouponRepository;

    public IssuedCouponFinder(IssuedCouponRepository issuedCouponRepository) {
        this.issuedCouponRepository = issuedCouponRepository;
    }

    public List<IssuedCoupon> findAllByPrincipalKey(String principalKey) {
        return issuedCouponRepository.findAllByPrincipalKey(principalKey);
    }

    public boolean existsByPrincipalKeyAndCouponId(Principal principal, Long couponId) {
        return issuedCouponRepository.existsByPrincipalKeyAndCouponId(principal.getKey(), couponId);
    }

    public List<IssuedCoupon> getUsableAllByPrincipalAndCoupons(Principal principal, List<Coupon> coupons) {
        return issuedCouponRepository.findAllByPrincipalKey(principal.getKey())
                .stream()
                .filter(issuedCoupon -> issuedCoupon.getState() == IssuedCouponState.DOWNLOADED)
                .filter(issuedCoupon -> coupons.contains(issuedCoupon.getCoupon()))
                .toList();

    }

    public IssuedCoupon getById(Long issuedCouponId) {
        return issuedCouponRepository.findById(issuedCouponId)
                .orElseThrow(()-> new CoreException(ErrorType.NOT_FOUND_DATA, null));
    }

}
