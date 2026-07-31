package com.coffee.gu.coupon;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.enums.CouponTargetType;

import com.coffee.gu.menu.Menu;
import com.coffee.gu.menu.MenuCategory;
import com.coffee.gu.menu.MenuCategoryRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Component
public class CouponFinder {

    private final CouponRepository couponRepository;
    private final CouponTargetRepository couponTargetRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final MenuCategoryRepository menuCategoryRepository;

    public CouponFinder(CouponRepository couponRepository, CouponTargetRepository couponTargetRepository, IssuedCouponRepository issuedCouponRepository, MenuCategoryRepository menuCategoryRepository) {
        this.couponRepository = couponRepository;
        this.couponTargetRepository = couponTargetRepository;
        this.issuedCouponRepository = issuedCouponRepository;
        this.menuCategoryRepository = menuCategoryRepository;
    }

    public List<Coupon> findApplicableCoupons(List<Menu> menus) {
        List<Long> menuIds = menus.stream().map(Menu::getId).toList();
        List<CouponTarget> menuTargets = couponTargetRepository.findAllByTargetTypeAndTargetIdIn(
                CouponTargetType.MENU,
                menuIds);
        List<CouponTarget> categoryTargets = couponTargetRepository.findAllByTargetTypeAndTargetIdIn(
                CouponTargetType.MENU_CATEGORY,
                menuCategoryRepository.findAllByMenuIdIn(menuIds)
                        .stream()
                        .map(MenuCategory::getCategoryId).toList()
        );
        List<Long> couponIds = Stream.concat(menuTargets.stream(), categoryTargets.stream())
                .map(CouponTarget::getCouponId)
                .toList();

        return findAllByIdIn(couponIds);
    }

    public List<Coupon> findDownloadedCoupons(String principalKey) {
        List<IssuedCoupon> issuedCoupons = issuedCouponRepository.findAllByPrincipalKey(principalKey);
        if (issuedCoupons.isEmpty()) return List.of();
        return couponRepository.findAllByIdIn(issuedCoupons.stream().map(issuedCoupon -> issuedCoupon.getCoupon().getId()).toList());
    }

    public List<Coupon> findAllByIdIn(List<Long> couponIds) {
        return couponRepository.findAllByIdIn(couponIds);
    }

    public Coupon getValidCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .filter(coupon -> coupon.getExpiredAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new CoreException(ErrorType.COUPON_NOT_FOUND_OR_EXPIRED, null));
    }

}
