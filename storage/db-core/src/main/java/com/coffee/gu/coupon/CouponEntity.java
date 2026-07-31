package com.coffee.gu.coupon;

import com.coffee.gu.enums.CouponType;
import com.coffee.gu.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table(name = "coupon")
@Entity
public class CouponEntity extends BaseEntity {

    private String name;
    private CouponType type;
    private BigDecimal discount;
    private LocalDateTime expiredAt;

    public CouponEntity() {}

    public CouponEntity(String name, CouponType type, BigDecimal discount, LocalDateTime expiredAt) {
        this.name = name;
        this.type = type;
        this.discount = discount;
        this.expiredAt = expiredAt;
    }

    public Coupon toModel() {
        return new Coupon(id, name, type, discount, expiredAt);
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
}
