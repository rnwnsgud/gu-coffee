package com.coffee.gu.stamp;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.StampState;

import java.time.LocalDateTime;

/**
 * - 음료 1잔당 스탬프 1개가 적립 (병음료, 디저트,md 제외)
 * - 스탬프의 유효기간은 적립일로부터 180일
 * - 스탬프 10개 적립시 리워드 쿠폰이 자동 발급되며, 유효기간은 발급일로부터 30일
 * - 리워드 쿠폰은 아메리카노 1잔 무료 교환이 가능하며, 제조음료 포함 주문시 1800원 금액권으로 사용이 가능 (사용시, 잔여 차액은 환불되지 않음)
 * - 리워드 쿠폰은 제조 음료 포함 주문 시 1장만 사용 가능
 */
public class Stamp {
    private Long id;
    private String orderKey;
    private Principal principal;
    private StampState state;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
    public static final int EXPIRY_ALARM_DAYS = 30;
    public static final int EXPIRY_DAYS = 180;

    public Stamp(Long id, String orderKey, Principal principal, StampState state, LocalDateTime createdAt, LocalDateTime expiredAt) {
        this.id = id;
        this.orderKey = orderKey;
        this.principal = principal;
        this.state = state;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
    }

    public static Stamp create(Principal principal, String orderKey, LocalDateTime expiredAt) {
        return new Stamp(
                null,
                orderKey,
                principal,
                StampState.EARNED,
                null,
                expiredAt
        );
    }

    public void use() {
        this.state = StampState.USED;
    }

    public void cancel() {
        this.state = StampState.CANCELED;
    }

    public Long getId() {
        return id;
    }

    public StampState getState() {
        return state;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public String getOrderKey() {
        return orderKey;
    }

}
