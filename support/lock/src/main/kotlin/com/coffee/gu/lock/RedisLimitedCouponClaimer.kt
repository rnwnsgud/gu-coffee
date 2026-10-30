package com.coffee.gu.lock

import org.redisson.api.RScript
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec

class RedisLimitedCouponClaimer(
    private val redissonClient: RedissonClient,
) : LimitedCouponClaimer {

    override fun claim(
        limitedCouponId: Long,
        principalKey: String,
        stockSupplier: (() -> Int)?,
    ): LimitedCouponClaimer.ClaimResult {
        val usersKey = usersKey(limitedCouponId)
        val stockKey = stockKey(limitedCouponId)

        var result = executeClaimScript(usersKey, stockKey, principalKey)

        if (result == NOT_INITIALIZED) {
            val initialStock = stockSupplier?.invoke() ?: 0
            val bucket = redissonClient.getBucket<String>(stockKey, StringCodec.INSTANCE)
            bucket.setIfAbsent(initialStock.toString())
            result = executeClaimScript(usersKey, stockKey, principalKey)
        }

        return when (result) {
            1L -> LimitedCouponClaimer.ClaimResult.SUCCESS
            0L -> LimitedCouponClaimer.ClaimResult.SOLD_OUT
            -1L -> LimitedCouponClaimer.ClaimResult.ALREADY_CLAIMED
            else -> LimitedCouponClaimer.ClaimResult.SOLD_OUT
        }
    }

    private fun executeClaimScript(usersKey: String, stockKey: String, principalKey: String): Long {
        val script = redissonClient.getScript(StringCodec.INSTANCE)
        val res = script.eval<Long>(
            RScript.Mode.READ_WRITE,
            CLAIM_LUA_SCRIPT,
            RScript.ReturnType.INTEGER,
            listOf(usersKey, stockKey),
            principalKey,
        )
        return res ?: 0L
    }

    override fun initializeStock(limitedCouponId: Long, totalQuantity: Int) {
        val stockKey = stockKey(limitedCouponId)
        val bucket = redissonClient.getBucket<String>(stockKey, StringCodec.INSTANCE)
        bucket.set(totalQuantity.toString())
    }

    override fun rollbackClaim(limitedCouponId: Long, principalKey: String) {
        val usersKey = usersKey(limitedCouponId)
        val stockKey = stockKey(limitedCouponId)
        val script = redissonClient.getScript(StringCodec.INSTANCE)
        script.eval<Long>(
            RScript.Mode.READ_WRITE,
            ROLLBACK_LUA_SCRIPT,
            RScript.ReturnType.INTEGER,
            listOf(usersKey, stockKey),
            principalKey,
        )
    }

    override fun getRemainingStock(limitedCouponId: Long): Int {
        val stockKey = stockKey(limitedCouponId)
        val bucket = redissonClient.getBucket<String>(stockKey, StringCodec.INSTANCE)
        return bucket.get()?.toIntOrNull() ?: 0
    }

    private fun usersKey(limitedCouponId: Long): String = "coupon:limited:users:$limitedCouponId"
    private fun stockKey(limitedCouponId: Long): String = "coupon:limited:stock:$limitedCouponId"

    companion object {
        private const val NOT_INITIALIZED = -2L

        private val CLAIM_LUA_SCRIPT = """
            if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then
                return -1
            end
            if redis.call('EXISTS', KEYS[2]) == 0 then
                return -2
            end
            local stock = redis.call('DECR', KEYS[2])
            if stock < 0 then
                redis.call('INCR', KEYS[2])
                return 0
            end
            redis.call('SADD', KEYS[1], ARGV[1])
            return 1
        """.trimIndent()

        private val ROLLBACK_LUA_SCRIPT = """
            local removed = redis.call('SREM', KEYS[1], ARGV[1])
            if removed == 1 then
                redis.call('INCR', KEYS[2])
                return 1
            end
            return 0
        """.trimIndent()
    }
}
