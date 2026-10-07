package com.coffee.gu.coupon

import org.springframework.data.jpa.repository.JpaRepository

interface LimitedCouponJpaRepository : JpaRepository<LimitedCouponEntity, Long>
