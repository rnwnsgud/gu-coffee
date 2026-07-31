package com.coffee.gu.coupon;

import com.coffee.gu.enums.CouponType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Coupon {
    private Long id;
    private String name;
    private CouponType type;
    private BigDecimal discount;
    private LocalDateTime expiredAt;

    public static final Long REWARD_COUPON_MASTER_ID = 1L;
    public static final String REWARD_COUPON_NAME = "리워드 쿠폰";
    public static final int REWARD_COUPON_STAMP_COUNT = 10;
    public static final int REWARD_COUPON_EXPIRY_DAYS = 30;
    public static final BigDecimal REWARD_COUPON_DISCOUNT_AMOUNT = new BigDecimal("1800");

    public Coupon(Long id, String name, CouponType type, BigDecimal discount, LocalDateTime expiredAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.discount = discount;
        this.expiredAt = expiredAt;
    }

    public static Coupon rewardCoupon() {
        return new Coupon(REWARD_COUPON_MASTER_ID, REWARD_COUPON_NAME, CouponType.FREE_DRINK, REWARD_COUPON_DISCOUNT_AMOUNT, LocalDateTime.now().plusDays(REWARD_COUPON_EXPIRY_DAYS));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coupon coupon = (Coupon) o;
        return id != null && id.equals(coupon.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CouponType getType() {
        return type;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public boolean isFreeDrinkReward() {
        return CouponType.FREE_DRINK.equals(this.type);
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (isFreeDrinkReward()) {
            return orderAmount.min(REWARD_COUPON_DISCOUNT_AMOUNT);
        }
        return discount;
    }
}

/**
 * // 교환권 정책 (어떤 상품을 교환할 수 있는가)
 * public class Voucher {
 *     private Long id;
 *     private String name;
 *     private Long targetMenuId; // 교환 대상 메뉴 ID
 *     private LocalDateTime expiredAt;
 * }
 *
 * // 사용자에게 발급된 교환권
 * public class IssuedVoucher {
 *     private Long id;
 *     private Long userId;
 *     private Long voucherId;
 *     private IssuedVoucherState status; // ISSUED, USED
 *     private LocalDateTime usedAt;
 *
 *     // 금액권 정의 (예: 1만원권, 3만원권)
 * public class CashCard {
 *     private Long id;
 *     private String name;
 *     private BigDecimal initialAmount; // 발행 금액
 * }
 *
 * // 사용자에게 발급된 금액권 (잔액 관리)
 * public class IssuedCashCard {
 *     private Long id;
 *     private Long userId;
 *     private Long cashCardId;
 *     private BigDecimal balance; // 현재 남은 잔액 (핵심!)
 *     private IssuedCashCardState status; // ACTIVE, EXPIRED, USED_UP
 *
 *     // 잔액 차감 로직 (도메인 메서드)
 *     public void use(BigDecimal orderAmount) {
 *         if (balance.compareTo(orderAmount) < 0) {
 *             throw new IllegalArgumentException("잔액이 부족합니다.");
 *         }
 *         this.balance = this.balance.subtract(orderAmount);
 *     }
 * }
 * }
 */