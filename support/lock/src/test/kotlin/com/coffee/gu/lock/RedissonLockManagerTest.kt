package com.coffee.gu.lock

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.redisson.client.RedisException

class RedissonLockManagerTest {

    private val redissonClient: RedissonClient = mock()
    private val rLock: RLock = mock()
    private val lockManager = RedissonLockManager(redissonClient)

    @Test
    @DisplayName("[Fail-Closed] Redis 장애로 락 획득 시 RedisException이 발생하면 비즈니스 로직을 실행하지 않고 즉시 COUPON_LOCK_ACQUISITION_FAILED 예외를 던져 요청을 차단한다")
    fun testFailClosedOnRedisFailure() {
        // given
        val lockKey = "COUPON:1"
        given(redissonClient.getLock("LOCK:$lockKey")).willReturn(rLock)
        given(rLock.tryLock(any(), any(), any())).willThrow(RedisException("Redis connection timed out"))

        val mockAction: () -> String = mock()

        // when & then
        val exception = assertThrows<CoreException> {
            lockManager.executeWithLock(lockKey) {
                mockAction()
            }
        }

        // 1. 비즈니스 로직 실행이 원천 차단되었는지 검증
        verify(mockAction, never()).invoke()

        // 2. 정확한 락 획득 실패 예외가 발생했는지 검증 (Fail-Closed)
        assertThat(exception.errorType).isEqualTo(ErrorType.COUPON_LOCK_ACQUISITION_FAILED)
    }

    @Test
    @DisplayName("[안전 해제] 비즈니스 로직이 성공한 후 unlock() 시점에 Redis 장애가 발생하더라도 정상 반환값을 보존하고 예외를 삼킨다")
    fun testUnlockFailureDoesNotMaskBusinessResult() {
        // given
        val lockKey = "COUPON:2"
        given(redissonClient.getLock("LOCK:$lockKey")).willReturn(rLock)
        given(rLock.tryLock(any(), any(), any())).willReturn(true)
        given(rLock.isLocked).willReturn(true)
        given(rLock.isHeldByCurrentThread).willReturn(true)
        given(rLock.unlock()).willThrow(RedisException("Redis disconnected during unlock"))

        // when: 비즈니스 로직 정상 실행
        val result = lockManager.executeWithLock(lockKey) {
            "SUCCESS_RESULT"
        }

        // then: finally의 unlock 예외에 의해 결과가 실패로 덮어써지지 않고 정상 반환 검증
        assertThat(result).isEqualTo("SUCCESS_RESULT")
    }
}
