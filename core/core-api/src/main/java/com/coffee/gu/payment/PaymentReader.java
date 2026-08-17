package com.coffee.gu.payment;


import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentReader {

    private final PaymentRepository paymentRepository;

    public PaymentReader(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment getByOrderKey(String orderKey) {
        return paymentRepository.findByOrderKey(orderKey)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA, null));
    }

    public Payment getByIdWithLock(Long paymentId) {
        return paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA, null));
    }

    public Payment getByOrderKeyWithLock(String orderKey) {
        return paymentRepository.findByOrderKeyWithLock(orderKey)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA, null));
    }

    public List<Payment> getPendingPayments(int limit) {
        return paymentRepository.getPendingPayments(limit);
    }
}
