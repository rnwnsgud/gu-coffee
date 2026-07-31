package com.coffee.gu;


public interface PaymentGateway {
    PaymentGatewayProvider provider();
    PGConfirmResult confirm(PaymentGatewayConfirm confirm);
    PGPayment getByOrderKey(String orderKey);
    PGCancelResult cancel(PaymentGatewayCancel cancel);

}
