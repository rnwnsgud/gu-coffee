package com.coffee.gu.toss;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.enums.PaymentMethod;

import org.springframework.stereotype.Component;

@Component
public class TossPaymentsMethodMapper {

    public PaymentMethod map(Payment payment) {
        if (isCard(payment.method())) {
            return PaymentMethod.CARD;
        }

        if (isEasyPay(payment.method())) {
            return mapEasyPayProvider(payment.easyPay());
        }

        throw new CoreException(ErrorType.DEFAULT_ERROR, "지원하지 않는 토스페이먼츠 결제수단입니다. method=" + payment.method());
    }

    private boolean isCard(String method) {
        if (method == null) {
            return false;
        }
        return method.equals("카드");
    }

    private boolean isEasyPay(String method) {
        if (method == null) {
            return false;
        }
        return method.equals("간편결제");
    }

    private PaymentMethod mapEasyPayProvider(Payment.EasyPay easyPay) {
        if (easyPay == null || easyPay.provider() == null) {
            throw new CoreException(ErrorType.DEFAULT_ERROR, "간편결제 provider 값이 없습니다.");
        }

        String provider = easyPay.provider();

        return switch (provider) {
            case "카카오페이", "KAKAOPAY" -> PaymentMethod.KAKAO_PAY;
            case "네이버페이", "NAVERPAY" -> PaymentMethod.NAVER_PAY;
            case "페이코", "PAYCO" -> PaymentMethod.PAYCO;
            case "토스페이", "TOSSPAY"   -> PaymentMethod.TOSS_PAY;
            default -> throw new CoreException(ErrorType.DEFAULT_ERROR, "지원하지 않는 간편결제 provider입니다. provider=" + easyPay.provider());
        };
    }

}
