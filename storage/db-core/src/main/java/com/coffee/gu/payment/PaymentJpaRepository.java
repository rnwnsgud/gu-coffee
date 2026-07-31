package com.coffee.gu.payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentEntity p WHERE p.id = :paymentId")
    Optional<PaymentEntity> findByIdForUpdate(Long paymentId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentEntity p WHERE p.orderKey = :orderKey")
    Optional<PaymentEntity> findByOrderIdForUpdate(String orderKey);
}
