package com.coffee.gu.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({AsyncTxTestPublisher.class, AsyncTxTestListener.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class AsyncTransactionalAopOrderTest {

    @Autowired
    private AsyncTxTestPublisher publisher;

    @Test
    @DisplayName("일반 @Async @EventListener는 메인 트랜잭션 커밋 전 비동기 스레드가 DB를 조회하여 데이터를 찾지 못하는 꼬임(유실) 현상이 발생한다")
    void testStandardAsyncEventListenerRaceCondition() throws Exception {
        // given
        String orderKey = "ORDER-RACE-" + UUID.randomUUID();

        // when
        CompletableFuture<Boolean> future = publisher.publishStandardAsyncEventInTransaction(orderKey);
        Boolean foundInAsyncThread = future.get(5, TimeUnit.SECONDS);

        // then
        // 메인 트랜잭션 커밋 전에 @Async 스레드가 DB 조회를 수행했으므로 data가 발견되지 않음 (false)
        assertThat(foundInAsyncThread).isFalse();
    }

    @Test
    @DisplayName("@TransactionalEventListener(AFTER_COMMIT) + @Async 적용 시 메인 트랜잭션 커밋 후 비동기 조회가 수행되어 데이터 꼬임 없이 100% 정상 조회된다")
    void testTransactionalEventListenerAfterCommitAsync() throws Exception {
        // given
        String orderKey = "ORDER-FIXED-" + UUID.randomUUID();

        // when
        CompletableFuture<Boolean> future = publisher.publishTransactionalAsyncEventInTransaction(orderKey);
        Boolean foundInAsyncThread = future.get(5, TimeUnit.SECONDS);

        // then
        // 메인 트랜잭션 커밋 완료 후에만 @Async 스레드가 실행되므로 데이터 100% 정상 발견 (true)
        assertThat(foundInAsyncThread).isTrue();
    }
}
