package com.coffee.gu.config

import com.coffee.gu.menu.MenuDetailResult
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.util.concurrent.Callable

@Configuration
@EnableCaching
class CacheConfig : CachingConfigurer {

    companion object {
        private val log = LoggerFactory.getLogger(CacheConfig::class.java)
        const val MENU_DETAIL_CACHE = "menuDetail"
    }

    @Bean
    @ConditionalOnProperty(name = ["spring.cache.type"], havingValue = "redis", matchIfMissing = true)
    fun redisCacheManager(
        redisConnectionFactory: RedisConnectionFactory
    ): CacheManager {
        val jsonMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

        val baseConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
            .entryTtl(Duration.ofMinutes(30))
            .disableCachingNullValues()

        val menuDetailSerializer = JacksonJsonRedisSerializer(jsonMapper, MenuDetailResult::class.java)
        val menuDetailConfig = baseConfiguration
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(menuDetailSerializer))
            .entryTtl(Duration.ofHours(1))

        val delegate = RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(baseConfiguration)
            .withCacheConfiguration(MENU_DETAIL_CACHE, menuDetailConfig)
            .build()

        return FailOpenCacheManager(delegate)
    }

    override fun errorHandler(): CacheErrorHandler {
        return object : CacheErrorHandler {
            override fun handleCacheGetError(exception: RuntimeException, cache: Cache, key: Any) {
                log.warn("Redis 캐시 조회 실패 (DB Fallback 수행 - Fail-Open): cache={}, key={}", cache.name, key, exception)
            }

            override fun handleCachePutError(exception: RuntimeException, cache: Cache, key: Any, value: Any?) {
                log.warn("Redis 캐시 저장 실패 (무시 - Fail-Open): cache={}, key={}", cache.name, key, exception)
            }

            override fun handleCacheEvictError(exception: RuntimeException, cache: Cache, key: Any) {
                log.warn("Redis 캐시 삭제 실패 (무시 - Fail-Open): cache={}, key={}", cache.name, key, exception)
            }

            override fun handleCacheClearError(exception: RuntimeException, cache: Cache) {
                log.warn("Redis 캐시 초기화 실패 (무시 - Fail-Open): cache={}", cache.name, exception)
            }
        }
    }

    private class FailOpenCacheManager(
        private val delegate: CacheManager,
    ) : CacheManager {
        override fun getCache(name: String): Cache? {
            return delegate.getCache(name)?.let { FailOpenCache(it) }
        }

        override fun getCacheNames(): Collection<String> = delegate.cacheNames
    }

    private class FailOpenCache(
        private val delegate: Cache,
    ) : Cache by delegate {
        override fun <T : Any> get(key: Any, valueLoader: Callable<T>): T? {
            return try {
                delegate.get(key, valueLoader)
            } catch (e: Cache.ValueRetrievalException) {
                val cause = e.cause
                if (cause is RuntimeException) throw cause
                if (cause is Error) throw cause
                throw e
            } catch (e: RuntimeException) {
                log.warn("Redis sync 캐시 조회 실패 (DB Fallback 수행 - Fail-Open): cache={}, key={}", delegate.name, key, e)
                valueLoader.call()
            }
        }
    }
}
