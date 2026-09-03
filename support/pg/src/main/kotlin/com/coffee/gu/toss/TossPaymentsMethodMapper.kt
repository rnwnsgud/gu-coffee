package com.coffee.gu.toss

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.PaymentMethod
import org.springframework.stereotype.Component

@Component
class TossPaymentsMethodMapper {

    fun map(payment: Payment): PaymentMethod {
        if (isCard(payment.method)) {
            return PaymentMethod.CARD
        }

        if (isEasyPay(payment.method)) {
            return mapEasyPayProvider(payment.easyPay)
        }

        throw CoreException(ErrorType.DEFAULT_ERROR, "지원하지 않는 토스페이먼츠 결제수단입니다. method=${payment.method}")
    }

    private fun isCard(method: String?): Boolean {
        return method == "카드"
    }

    private fun isEasyPay(method: String?): Boolean {
        return method == "간편결제"
    }

    private fun mapEasyPayProvider(easyPay: Payment.EasyPay?): PaymentMethod {
        val provider = easyPay?.provider
            ?: throw CoreException(ErrorType.DEFAULT_ERROR, "간편결제 provider 값이 없습니다.")

        return when (provider) {
            "카카오페이", "KAKAOPAY" -> PaymentMethod.KAKAO_PAY
            "네이버페이", "NAVERPAY" -> PaymentMethod.NAVER_PAY
            "페이코", "PAYCO" -> PaymentMethod.PAYCO
            "토스페이", "TOSSPAY" -> PaymentMethod.TOSS_PAY
            else -> throw CoreException(ErrorType.DEFAULT_ERROR, "지원하지 않는 간편결제 provider입니다. provider=$provider")
        }
    }
}
