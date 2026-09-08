package com.coffee.gu.cancel

import com.coffee.gu.payment.Payment
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class CancelRepositoryImpl(
    private val cancelJpaRepository: CancelJpaRepository,
) : CancelRepository {

    override fun cancel(payment: Payment, externalCancelKey: String): Cancel {
        return cancelJpaRepository.save(
            CancelEntity(
                principalKey = payment.principal.key,
                principalType = payment.principal.type,
                orderKey = payment.orderKey,
                paymentId = payment.id ?: 0L,
                originalAmount = payment.originalAmount,
                issuedCouponId = payment.issuedCouponId,
                couponDiscount = payment.couponDiscount,
                paidAmount = payment.amount,
                canceledAmount = payment.amount,
                externalCancelKey = externalCancelKey,
                canceledAt = OffsetDateTime.now(),
            ),
        ).toModel()
    }
}
