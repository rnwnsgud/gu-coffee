package com.coffee.gu.cancel;

import com.coffee.gu.payment.Payment;

import org.springframework.stereotype.Component;

@Component
public class CancelManager {

    private final CancelRepository cancelRepository;

    public CancelManager(CancelRepository cancelRepository) {
        this.cancelRepository = cancelRepository;
    }

    public Cancel cancel(Payment payment, String externalCancelKey) {
        return cancelRepository.cancel(payment, externalCancelKey);
    }
}
