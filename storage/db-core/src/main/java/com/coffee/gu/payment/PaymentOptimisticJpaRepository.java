package com.coffee.gu.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentOptimisticJpaRepository extends JpaRepository<PaymentOptimisticEntity, Long> {
    Optional<PaymentOptimisticEntity> findByOrderKey(String orderKey);
}
