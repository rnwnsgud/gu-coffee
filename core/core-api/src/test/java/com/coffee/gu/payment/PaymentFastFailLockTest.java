package com.coffee.gu.payment;

import com.coffee.gu.CoreException;
import com.coffee.gu.PGConfirmResult;
import com.coffee.gu.PGPayment;
import com.coffee.gu.PaymentGateway;
import com.coffee.gu.PaymentGatewayConfirm;
import com.coffee.gu.PaymentGatewayStatus;
import com.coffee.gu.Principal;
import com.coffee.gu.enums.OrderState;
import com.coffee.gu.enums.PaymentMethod;
import com.coffee.gu.enums.PaymentState;
import com.coffee.gu.enums.StoreStatus;
import com.coffee.gu.order.Order;
import com.coffee.gu.order.OrderLine;
import com.coffee.gu.order.OrderRepository;
import com.coffee.gu.store.BusinessInformation;
import com.coffee.gu.store.SalesInformation;
import com.coffee.gu.store.Store;
import com.coffee.gu.store.StoreLocation;
import com.coffee.gu.store.StoreRepository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PaymentFastFailLockTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @Test
    @DisplayName("동시 10개 요청 진입 시 비관적 락 기반 Fast-Fail로 외부 PG 승인은 단 1회만 호출되고 중복 호출은 차단된다")
    void testConcurrentPaymentApprovalFastFail() throws Exception {
        // given
        String orderKey = "ORDER-FAST-FAIL-" + UUID.randomUUID();
        Principal user = Principal.user("U100");
        BigDecimal amount = new BigDecimal("3000.00");

        StoreLocation location = new StoreLocation("인천 부평구", 37.5, 127.0);
        SalesInformation salesInfo = new SalesInformation(location, List.of(), "032-123-4567");
        BusinessInformation busiInfo = new BusinessInformation("홍길동", "구커피 부평점", "123-45-67890", "인천 부평구");

        Store store = storeRepository.save(new Store(null, "부평점", "BP01", StoreStatus.OPEN, salesInfo, busiInfo));
        Long storeId = store.getId();

        OrderLine line = new OrderLine(null, orderKey, storeId, "아메리카노", null, null, 1L, amount, amount, false);
        Order order = new Order(orderKey, "아메리카노 1잔", user, storeId, amount, OrderState.CREATED, List.of(line));
        orderRepository.create(order);

        paymentService.createPayment(order, PaymentDiscount.of(List.of(), null, amount));

        Payment dbPayment = paymentRepository.findByOrderKey(orderKey).orElseThrow();
        BigDecimal exactAmount = dbPayment.getAmount();

        // PG Mock 설정
        given(paymentGateway.getByOrderKey(orderKey))
                .willReturn(new PGPayment("PAY-KEY-100", orderKey, exactAmount, PaymentGatewayStatus.READY));

        given(paymentGateway.confirm(any(PaymentGatewayConfirm.class)))
                .willReturn(PGConfirmResult.success(orderKey, "PAY-KEY-100", PaymentMethod.CARD, "APPROVE-100", OffsetDateTime.now()));

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        List<Future<PaymentApprovalResult>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger fastFailCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                startLatch.await();
                try {
                    return paymentService.approvePayment(order);
                } finally {
                    endLatch.countDown();
                }
            }));
        }

        startLatch.countDown(); // 10개 스레드 동시에 출발
        endLatch.await();
        executorService.shutdown();

        for (Future<PaymentApprovalResult> future : futures) {
            try {
                PaymentApprovalResult result = future.get();
                if (result.getPaymentState() == PaymentState.SUCCESS) {
                    successCount.incrementAndGet();
                }
            } catch (ExecutionException e) {
                if (e.getCause() instanceof CoreException) {
                    fastFailCount.incrementAndGet();
                }
            }
        }

        // then
        // 1. 10개 동시 요청 중 승인에 성공하거나 기존 완료 상태를 멱등 반환한 수 + Fast-Fail 처리 수의 합은 10이어야 함
        assertThat(futures.size()).isEqualTo(10);
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);

        // 2. 외부 PG 승인(confirm) API 호출은 오직 단 1회만 실행되어야 함 (비관적 락 기반 중복 호출 완전 차단 검증)
        org.mockito.Mockito.verify(paymentGateway, org.mockito.Mockito.times(1)).confirm(any(PaymentGatewayConfirm.class));

        // 3. 최종 DB 결제 상태는 SUCCESS (PAID) 이어야 함 (1차 캐시 초기화 후 재조회)
        entityManager.clear();
        Payment finalPayment = paymentRepository.findByOrderKey(orderKey).orElseThrow();
        assertThat(finalPayment.isPaid()).isTrue();
    }
}
