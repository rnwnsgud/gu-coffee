package com.coffee.gu.cancel;

import com.coffee.gu.payment.Payment;

public interface CancelRepository {
    Cancel cancel(Payment payment, String externalCancelKey);
}
