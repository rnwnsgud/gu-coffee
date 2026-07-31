package com.coffee.gu.toss;


import com.coffee.gu.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class TossPaymentsGateway implements PaymentGateway {

    private final RestClient restClient;
    private final TossPaymentsProperties properties;
    private final TossPaymentsMethodMapper methodMapper;

    private static final Logger log = LoggerFactory.getLogger(TossPaymentsGateway.class);

    public TossPaymentsGateway(RestClient restClient, TossPaymentsProperties properties, TossPaymentsMethodMapper methodMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.methodMapper = methodMapper;
    }

    @Override
    public PaymentGatewayProvider provider() {
        return PaymentGatewayProvider.TOSS_PAYMENTS;
    }

    @Override
    public PGConfirmResult confirm(PaymentGatewayConfirm confirm) {
        TossPaymentsConfirmRequest request = new TossPaymentsConfirmRequest(confirm.paymentKey(), confirm.orderKey(), confirm.amount());
        try {
            Payment response = restClient.post()
                    .uri(properties.baseURl() + "/payments/confirm")
                    .header("Authorization", basicAuthorizationHeader(properties.secretKey()))
                    .header("Idempotency-Key", confirm.paymentKey())
                    .header("Content-Type", "application/json")
                    .body(request)
                    .retrieve()
                    .body(Payment.class);
            if (response == null) {
                throw new CoreException(ErrorType.PAYMENT_FAIL, "토스페이먼츠 승인 응답이 비어 있습니다.");
            }
            return PGConfirmResult.success(
                    response.orderId(),
                    response.paymentKey(),
                    methodMapper.map(response),
                    response.card() != null? response.card().approveNo() : null,
                    response.approvedAt()
            );
        } catch (RestClientResponseException e) { // todo 예외처리 세분화?
            if (e.getStatusCode().is4xxClientError()) {
                return PGConfirmResult.fail(request.orderId(), request.paymentKey());
            }
            throw e;
        }
    }

    @Override
    public PGPayment getByOrderKey(String orderKey) {
        Payment response = restClient.get()
                .uri(properties.baseURl() + "/payments/orders/" + orderKey)
                .header("Authorization", basicAuthorizationHeader(properties.secretKey()))
                .retrieve()
                .body(Payment.class);
        if (response == null) {
            throw new CoreException(ErrorType.PAYMENT_FAIL, "토스페이먼츠 승인 응답이 비어 있습니다.");
        }
        return new PGPayment(
                response.paymentKey(),
                response.orderId(),
                response.totalAmount(),
                PaymentGatewayStatus.valueOf(response.status().name())
        );
    }

    @Override
    public PGCancelResult cancel(PaymentGatewayCancel cancel) {
        TossPaymentsCancelRequest request = new TossPaymentsCancelRequest(cancel.cancelReason());
        try {
            Payment response = restClient.post()
                    .uri(properties.baseURl() + "/payments/" + cancel.paymentKey() + "/cancel")
                    .header("Authorization", basicAuthorizationHeader(properties.secretKey()))
                    .header("Idempotency-Key", cancel.paymentKey())
                    .body(request)
                    .retrieve()
                    .body(Payment.class);
            if (response == null) {
                throw new CoreException(ErrorType.PAYMENT_FAIL, "토스페이먼츠 승인 응답이 비어 있습니다.");
            }
            return new PGCancelResult(

            );
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
            }
            throw e;
        }
    }

    private String basicAuthorizationHeader(String secretKey) {
        String credential = secretKey + ":";
        String encodedCredential = Base64.getEncoder()
                .encodeToString(credential.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredential;
    }

}
