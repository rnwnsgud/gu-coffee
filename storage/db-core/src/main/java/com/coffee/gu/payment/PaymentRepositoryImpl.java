package com.coffee.gu.payment;

import com.coffee.gu.enums.PaymentState;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.coffee.gu.payment.QPaymentEntity.paymentEntity;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

    private final JPAQueryFactory queryFactory;
    private final PaymentJpaRepository paymentJpaRepository;

    public PaymentRepositoryImpl(PaymentJpaRepository paymentJpaRepository, JPAQueryFactory queryFactory) {
        this.paymentJpaRepository = paymentJpaRepository;
        this.queryFactory = queryFactory;
    }

    @Override
    public Optional<Payment> findByOrderKey(String orderKey) {
        return Optional.ofNullable(
                        queryFactory.selectFrom(paymentEntity)
                                .where(paymentEntity.orderKey.eq(orderKey))
                                .fetchFirst()
                ).map(PaymentEntity::toModel);
    }

    @Override
    public Payment save(Payment payment) {
        return paymentJpaRepository.save(PaymentEntity.from(payment)).toModel();
    }

    @Override
    public Optional<Payment> findByIdWithLock(Long id) {
        return paymentJpaRepository.findByIdForUpdate(id)
                .map(PaymentEntity::toModel);
    }

    @Override
    public Optional<Payment> findByOrderKeyWithLock(String orderKey) {
        return paymentJpaRepository.findByOrderIdForUpdate(orderKey)
                .map(PaymentEntity::toModel);
    }

    @Override
    public List<Payment> getPendingPayments() {
        LocalDateTime createdBefore = LocalDateTime.now().minusMinutes(5);
        return queryFactory.selectFrom(paymentEntity)
                .where(paymentEntity.state.eq(PaymentState.PENDING_PG),
                        paymentEntity.updatedAt.before(createdBefore))
                .fetch()
                .stream()
                .map(PaymentEntity::toModel)
                .toList();
    }
}
