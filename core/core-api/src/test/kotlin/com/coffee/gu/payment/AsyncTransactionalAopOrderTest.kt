package com.coffee.gu.payment

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import java.util.UUID
import java.util.concurrent.TimeUnit

@SpringBootTest
@Import(AsyncTxTestPublisher::class, AsyncTxTestListener::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AsyncTransactionalAopOrderTest {

    @Autowired
    private lateinit var publisher: AsyncTxTestPublisher

    @Test
    @DisplayName("일반 @Async @EventListener는 메인 트랜잭션 커밋 전 비동기 스레드가 DB를 조회하여 데이터를 찾지 못하는 꼬임(유실) 현상이 발생한다")
    fun testStandardAsyncEventListenerRaceCondition() {
        // given
        val orderKey = "ORDER-RACE-" + UUID.randomUUID()

        // when
        val future = publisher.publishStandardAsyncEventInTransaction(orderKey)
        val foundInAsyncThread = future.get(5, TimeUnit.SECONDS)

        // then
        assertThat(foundInAsyncThread).isFalse()
    }

    @Test
    @DisplayName("@TransactionalEventListener(AFTER_COMMIT) + @Async 적용 시 메인 트랜잭션 커밋 후 비동기 조회가 수행되어 데이터 꼬임 없이 100% 정상 조회된다")
    fun testTransactionalEventListenerAfterCommitAsync() {
        // given
        val orderKey = "ORDER-FIXED-" + UUID.randomUUID()

        // when
        val future = publisher.publishTransactionalAsyncEventInTransaction(orderKey)
        val foundInAsyncThread = future.get(5, TimeUnit.SECONDS)

        // then
        assertThat(foundInAsyncThread).isTrue()
    }
}
