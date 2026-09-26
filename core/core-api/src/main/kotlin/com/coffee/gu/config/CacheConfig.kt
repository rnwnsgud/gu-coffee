package com.coffee.gu.config

import com.coffee.gu.menu.MenuDetailResult
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
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

@Configuration
@EnableCaching
class CacheConfig {

    companion object {
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

        val menuDetailSerializer = JacksonJsonRedisSerializer(jsonMapper, MenuDetailResult::class.java)

        val defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(menuDetailSerializer))
            .entryTtl(Duration.ofHours(1))
            .disableCachingNullValues()

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfiguration)
            .withCacheConfiguration(MENU_DETAIL_CACHE, defaultConfiguration)
            .build()
    }
}
