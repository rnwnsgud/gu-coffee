package com.coffee.gu.coupon;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CouponJpaRepository extends JpaRepository<CouponEntity, Long> {
    Optional<CouponEntity> findByIdAndExpiredAtAfter(Long id, LocalDateTime expiredAt);
    List<CouponEntity> findByIdIn(Collection<Long> ids);
}
