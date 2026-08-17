package com.coffee.gu.payment;

import com.coffee.gu.enums.PaymentState;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import org.hibernate.jpa.AvailableHints;
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

    // todo 다음과 같은 상황이 오면 Redis 분산 락 도입을 고려하시면 됩니다:
    //블랙프라이데이처럼 0.1초 만에 수만 명이 몰려 DB 커넥션 풀이 터질 것 같아서, DB로 트래픽이 들어가기 전에 Redis에서 1차로 막고 싶을 때
    //PG사 승인 API처럼 시간이 오래 걸리는 외부 네트워크 통신을 DB 락 없이 외부에서 상호 배제하고 싶을 때
    // todo 현재 시스템 상 서버를 확장할때 문제점이 있을지 확인하기
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
    public List<Payment> getPendingPayments(int limit) {
        LocalDateTime createdBefore = LocalDateTime.now().minusMinutes(5);
        return queryFactory.selectFrom(paymentEntity)
                .where(paymentEntity.state.eq(PaymentState.PENDING_PG),
                        paymentEntity.updatedAt.before(createdBefore),
                        paymentEntity.retryCount.lt(5))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint(AvailableHints.HINT_SPEC_LOCK_TIMEOUT, -2) // skip lock
                .limit(limit)
                .fetch()
                .stream()
                .map(PaymentEntity::toModel)
                .toList();
    }
}
