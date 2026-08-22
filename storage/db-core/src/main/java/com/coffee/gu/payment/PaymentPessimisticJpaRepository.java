package com.coffee.gu.payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentPessimisticJpaRepository extends JpaRepository<PaymentPessimisticEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentPessimisticEntity p WHERE p.orderKey = :orderKey")
    Optional<PaymentPessimisticEntity> findByOrderKeyForUpdate(@Param("orderKey") String orderKey);
}
