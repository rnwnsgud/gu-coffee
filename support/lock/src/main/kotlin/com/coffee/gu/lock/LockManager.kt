package com.coffee.gu.lock

import java.util.concurrent.TimeUnit

interface LockManager {
    fun <T> executeWithLock(
        key: String,
        waitTime: Long = DEFAULT_WAIT_TIME,
        leaseTime: Long = DEFAULT_LEASE_TIME,
        timeUnit: TimeUnit = TimeUnit.SECONDS,
        action: () -> T
    ): T

    companion object {
        const val DEFAULT_WAIT_TIME = 3L
        const val DEFAULT_LEASE_TIME = 5L
    }
}
