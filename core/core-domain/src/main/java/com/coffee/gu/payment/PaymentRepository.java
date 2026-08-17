package com.coffee.gu.payment;


import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    Optional<Payment> findByOrderKey(String orderKey);
    Payment save(Payment payment);
    Optional<Payment> findByIdWithLock(Long id);
    Optional<Payment> findByOrderKeyWithLock(String orderKey);
    List<Payment> getPendingPayments(int limit);
}
