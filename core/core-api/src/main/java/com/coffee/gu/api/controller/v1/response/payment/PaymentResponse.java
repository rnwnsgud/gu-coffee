package com.coffee.gu.api.controller.v1.response.payment;

import com.coffee.gu.enums.PaymentState;

public record PaymentResponse(PaymentState result) {
}
