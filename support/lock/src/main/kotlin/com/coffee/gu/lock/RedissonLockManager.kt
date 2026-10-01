package com.coffee.gu.lock

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import org.redisson.api.RedissonClient
import java.util.concurrent.TimeUnit

class RedissonLockManager(
    private val redissonClient: RedissonClient
) : LockManager {

    override fun <T> executeWithLock(
        key: String,
        waitTime: Long,
        leaseTime: Long,
        timeUnit: TimeUnit,
        action: () -> T
    ): T {
        val lockKey = LOCK_PREFIX + key
        val lock = redissonClient.getLock(lockKey)

        val acquired = runCatching {
            lock.tryLock(waitTime, leaseTime, timeUnit)
        }.getOrElse { false }

        if (!acquired) {
            throw CoreException(ErrorType.COUPON_LOCK_ACQUISITION_FAILED)
        }

        try {
            return action()
        } finally {
            if (lock.isLocked && lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }

    companion object {
        private const val LOCK_PREFIX = "LOCK:"
    }
}
