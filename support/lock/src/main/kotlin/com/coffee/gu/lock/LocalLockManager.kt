package com.coffee.gu.lock

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

class LocalLockManager : LockManager {
    private val locks = ConcurrentHashMap<String, ReentrantLock>()

    override fun <T> executeWithLock(
        key: String,
        waitTime: Long,
        leaseTime: Long,
        timeUnit: TimeUnit,
        action: () -> T
    ): T {
        val lock = locks.computeIfAbsent(key) { ReentrantLock() }
        val acquired = lock.tryLock(waitTime, timeUnit)
        if (!acquired) {
            throw CoreException(ErrorType.COUPON_LOCK_ACQUISITION_FAILED)
        }
        try {
            return action()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }
}
