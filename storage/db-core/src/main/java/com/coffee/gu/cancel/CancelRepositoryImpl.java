package com.coffee.gu.cancel;

import com.coffee.gu.payment.Payment;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public class CancelRepositoryImpl implements CancelRepository {

    private final CancelJpaRepository cancelJpaRepository;

    public CancelRepositoryImpl(CancelJpaRepository cancelJpaRepository) {
        this.cancelJpaRepository = cancelJpaRepository;
    }

    @Override
    public Cancel cancel(Payment payment, String externalCancelKey) {
        return cancelJpaRepository.save(
                new CancelEntity(
                        payment.getPrincipal().getKey(),
                        payment.getPrincipal().getType(),
                        payment.getOrderKey(),
                        payment.getId(),
                        payment.getOriginalAmount(),
                        payment.getIssuedCouponId(),
                        payment.getCouponDiscount(),
                        payment.getAmount(),
                        payment.getAmount(),
                        externalCancelKey,
                        OffsetDateTime.now()
                )
        ).toModel();
    }
}
