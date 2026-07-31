package com.coffee.gu.coupon;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssuedCouponJpaRepository extends JpaRepository<IssuedCouponEntity, Long> {
}
