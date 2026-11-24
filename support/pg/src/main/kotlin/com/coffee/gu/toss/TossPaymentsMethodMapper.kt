package com.coffee.gu.toss

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.PaymentMethod
import org.springframework.stereotype.Component

@Component
class TossPaymentsMethodMapper {

    fun map(payment: Payment): PaymentMethod {
        if (isEasyPay(payment.method) && payment.easyPay?.provider.isNullOrBlank()) {
            throw CoreException(ErrorType.DEFAULT_ERROR, "간편결제 provider 값이 없습니다.")
        }

        return mapOrNull(payment.method, payment.easyPay?.provider)
            ?: if (isEasyPay(payment.method)) {
                throw CoreException(
                    ErrorType.DEFAULT_ERROR,
                    "지원하지 않는 간편결제 provider입니다. provider=${payment.easyPay?.provider}",
                )
            } else {
                throw CoreException(ErrorType.DEFAULT_ERROR, "지원하지 않는 토스페이먼츠 결제수단입니다. method=${payment.method}")
            }
    }

    fun mapOrNull(rawMethod: String?, easyPayProvider: String? = null): PaymentMethod? {
        val normalizedMethod = rawMethod?.trim()?.uppercase() ?: return null
        return when (normalizedMethod) {
            "카드", "CARD" -> PaymentMethod.CARD
            "가상계좌", "VIRTUAL_ACCOUNT" -> PaymentMethod.VIRTUAL_ACCOUNT
            "간편결제", "EASY_PAY" -> mapEasyPayProviderOrNull(easyPayProvider)
            else -> mapEasyPayProviderOrNull(normalizedMethod)
        }
    }

    private fun isEasyPay(method: String?): Boolean {
        val normalized = method?.trim()?.uppercase()
        return normalized == "간편결제" || normalized == "EASY_PAY"
    }

    private fun mapEasyPayProviderOrNull(provider: String?): PaymentMethod? {
        return when (provider?.trim()?.uppercase()) {
            "카카오페이", "KAKAOPAY", "KAKAO_PAY" -> PaymentMethod.KAKAO_PAY
            "네이버페이", "NAVERPAY", "NAVER_PAY" -> PaymentMethod.NAVER_PAY
            "페이코", "PAYCO" -> PaymentMethod.PAYCO
            "토스페이", "TOSSPAY", "TOSS_PAY" -> PaymentMethod.TOSS_PAY
            else -> null
        }
    }
}
