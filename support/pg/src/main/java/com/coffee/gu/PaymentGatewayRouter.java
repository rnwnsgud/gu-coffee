package com.coffee.gu;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

//@Component
public class PaymentGatewayRouter {

    private final Map<PaymentGatewayProvider, PaymentGateway> gateways;

    public PaymentGatewayRouter(List<PaymentGateway> paymentGateways) {
        this.gateways = new EnumMap<>(PaymentGatewayProvider.class);
        for (PaymentGateway paymentGateway : paymentGateways) {
            this.gateways.put(paymentGateway.provider(), paymentGateway);
        }
    }

    public PaymentGateway get(PaymentGatewayProvider provider) {
        PaymentGateway paymentGateway = gateways.get(provider);
        if (paymentGateway == null) {
            throw new CoreException(ErrorType.NOT_FOUND_DATA, null);
        }
        return paymentGateway;
    }
}
