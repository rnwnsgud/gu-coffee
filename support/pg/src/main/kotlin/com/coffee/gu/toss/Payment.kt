package com.coffee.gu.toss

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
class Payment(
    val paymentKey: String,
    val orderId: String,
    val status: TossPaymentsStatus,
    val totalAmount: BigDecimal,
    val method: String? = null,
    val card: Card? = null,
    val easyPay: EasyPay? = null,
    val approvedAt: OffsetDateTime? = null,
    val cancels: List<Cancel>? = null
) {
    class Card(
        val approveNo: String? = null
    )

    class EasyPay(
        val provider: String? = null,
        val amount: BigDecimal? = null,
        val discountAmount: BigDecimal? = null
    )

    class Cancel(
        val cancelAmount: BigDecimal? = null,
        val cancelReason: String? = null,
        val taxFreeAmount: BigDecimal? = null,
        val taxExemptionAmount: BigDecimal? = null,
        val refundableAmount: BigDecimal? = null,
        val cardDiscountAmount: BigDecimal? = null,
        val transferDiscountAmount: BigDecimal? = null,
        val easyPayDiscountAmount: BigDecimal? = null,
        val canceledAt: OffsetDateTime? = null,
        val transactionKey: String? = null,
        val receiptKey: String? = null,
        val cancelStatus: String? = null
    )
}
