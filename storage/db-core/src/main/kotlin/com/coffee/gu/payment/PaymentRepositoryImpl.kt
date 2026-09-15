package com.coffee.gu.payment

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.PaymentState
import com.coffee.gu.payment.QPaymentEntity.paymentEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import org.hibernate.jpa.AvailableHints
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.Optional

@Repository
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : PaymentRepository {

    override fun findByOrderKey(orderKey: String): Payment {
        return queryFactory.selectFrom(paymentEntity)
            .where(paymentEntity.orderKey.eq(orderKey))
            .fetchFirst()
            ?.toModel() ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
    }

    override fun save(payment: Payment): Payment {
        return paymentJpaRepository.save(PaymentEntity.from(payment)).toModel()
    }

    override fun findByIdWithLock(id: Long): Payment {
        return paymentJpaRepository.findByIdForUpdate(id)?.toModel() ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
    }

    override fun findByOrderKeyWithLock(orderKey: String): Payment {
        return paymentJpaRepository.findByOrderIdForUpdate(orderKey)?.toModel() ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
    }

    @Transactional(propagation = Propagation.MANDATORY)
    override fun claimPendingPayments(limit: Int): List<Payment> {
        val createdBefore = LocalDateTime.now().minusMinutes(5)
        val entities = queryFactory.selectFrom(paymentEntity)
            .where(
                paymentEntity.state.eq(PaymentState.PENDING_PG),
                paymentEntity.updatedAt.before(createdBefore),
                paymentEntity.retryCount.lt(5),
            )
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setHint(AvailableHints.HINT_SPEC_LOCK_TIMEOUT, -2)
            .limit(limit.toLong())
            .fetch()

        if (entities.isEmpty()) return emptyList()

        val ids = entities.map { it.id }
        queryFactory.update(paymentEntity)
            .set(paymentEntity.updatedAt, LocalDateTime.now())
            .where(paymentEntity.id.`in`(ids))
            .execute()

        return entities.map { it.toModel() }
    }
}
