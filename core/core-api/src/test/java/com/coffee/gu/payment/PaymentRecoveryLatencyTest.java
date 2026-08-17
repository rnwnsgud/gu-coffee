package com.coffee.gu.payment;

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
import com.coffee.gu.support.pg.PaymentRecoverScheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PaymentRecoveryLatencyTest {

    @Autowired
    private PaymentRecoverScheduler paymentRecoverScheduler;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @Test
    @DisplayName("복구 대상 건수별(targetCount) 실제 1건당 평균 처리 시간(Latency) 측정")
    void measurePaymentRecoveryLatency() throws Exception {
        // given
        int targetCount = 72;
        String prefix = UUID.randomUUID().toString().substring(0, 8);

        StoreLocation location = new StoreLocation("인천 부평구", 37.5, 127.0);
        SalesInformation salesInfo = new SalesInformation(location, List.of(), "032-123-4567");
        BusinessInformation busiInfo = new BusinessInformation("홍길동", "구커피 부평점", "123-45-67890", "인천 부평구");
        Store store = storeRepository.save(new Store(null, "부평점", "BP01", StoreStatus.OPEN, salesInfo, busiInfo));

        for (int i = 0; i < targetCount; i++) {
            String orderKey = "ORDER-LATENCY-" + prefix + "-" + i;
            Principal user = Principal.user("U-" + prefix + "-" + i);
            BigDecimal amount = new BigDecimal("3000.00");

            OrderLine line = new OrderLine(null, orderKey, store.getId(), "아메리카노", null, null, 1L, amount, amount, false);
            Order order = new Order(orderKey, "아메리카노 1잔", user, store.getId(), amount, OrderState.CREATED, List.of(line));
            orderRepository.create(order);

            paymentRepository.save(new Payment(
                    null,
                    user,
                    orderKey,
                    amount,
                    null,
                    BigDecimal.ZERO,
                    amount,
                    PaymentState.PENDING_PG,
                    "PAY-KEY-" + prefix + "-" + i,
                    PaymentMethod.CARD,
                    null,
                    null,
                    LocalDateTime.now().minusMinutes(10),
                    0
            ));
        }

        // updatedAt을 5분 이전으로 세팅 (Auditing 우회)
        jdbcTemplate.update("UPDATE payment SET updated_at = ?", Timestamp.valueOf(LocalDateTime.now().minusMinutes(10)));

        // PG사 외부 REST API 네트워크 RTT (실무 카드사/VAN사 통신 포함 평균 200ms 지연) Mocking
        given(paymentGateway.getByOrderKey(any(String.class)))
                .willAnswer(invocation -> {
                    Thread.sleep(200); // 실무 PG/카드사 HTTP REST RTT 200ms 모사
                    String orderKey = invocation.getArgument(0);
                    return new PGPayment("PAY-KEY-100", orderKey, new BigDecimal("3000.00"), PaymentGatewayStatus.DONE);
                });

        given(paymentGateway.confirm(any(PaymentGatewayConfirm.class)))
                .willReturn(PGConfirmResult.success("ORDER-KEY", "PAY-KEY-100", PaymentMethod.CARD, "APPROVE-100", OffsetDateTime.now()));

        // when (복구 스케줄러 실행 및 1건당 Latency 수집)
        long totalStartTime = System.nanoTime();

        // 스케줄러 실행
        paymentRecoverScheduler.schedule();

        long totalElapsedTimeMs = (System.nanoTime() - totalStartTime) / 1_000_000;

        // then
        // 지연 시간 통계 산출
        double avgLatencyPerItem = (double) totalElapsedTimeMs / targetCount;

        System.out.println("====== [결제 복구 Latency 실측 결과] ======");
        System.out.println("총 처리 건수: " + targetCount + "건");
        System.out.println("총 소요 시간: " + totalElapsedTimeMs + " ms");
        System.out.println("1건당 평균 처리 시간: " + String.format("%.2f", avgLatencyPerItem) + " ms");
        System.out.println("=========================================");

        assertThat(avgLatencyPerItem).isGreaterThanOrEqualTo(30.0); // PG사 네트워크 30ms 지연 포함
    }
}
