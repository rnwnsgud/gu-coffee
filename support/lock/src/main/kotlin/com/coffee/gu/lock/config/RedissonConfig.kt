package com.coffee.gu.lock.config

import com.coffee.gu.lock.LocalLockManager
import com.coffee.gu.lock.LockManager
import com.coffee.gu.lock.RedissonLockManager
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedissonConfig(
    @param:Value("\${spring.data.redis.host:localhost}")
    private val redisHost: String,
    @param:Value("\${spring.data.redis.port:6379}")
    private val redisPort: Int,
) {
    @Bean
    @ConditionalOnProperty(name = ["spring.lock.type"], havingValue = "redisson", matchIfMissing = true)
    fun redissonClient(): RedissonClient {
        val config = Config()
        config.useSingleServer()
            .setAddress("redis://$redisHost:$redisPort")
            .setConnectionMinimumIdleSize(5)
            .setConnectionPoolSize(20)
        return Redisson.create(config)
    }

    @Bean
    @ConditionalOnProperty(name = ["spring.lock.type"], havingValue = "redisson", matchIfMissing = true)
    fun redissonLockManager(redissonClient: RedissonClient): LockManager {
        return RedissonLockManager(redissonClient)
    }

    @Bean
    @ConditionalOnProperty(name = ["spring.lock.type"], havingValue = "local", matchIfMissing = false)
    fun localLockManager(): LockManager {
        return LocalLockManager()
    }
}
