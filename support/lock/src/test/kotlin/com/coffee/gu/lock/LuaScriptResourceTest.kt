package com.coffee.gu.lock

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.core.io.ClassPathResource

class LuaScriptResourceTest {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "scripts/reserve_stock.lua",
            "scripts/verify_and_confirm_stock.lua",
            "scripts/release_stock.lua",
            "scripts/available_stock.lua",
        ]
    )
    @DisplayName("Lua 스크립트 파일이 클래스패스에 존재하고 내용이 비어있지 않아야 한다")
    fun scriptFilesMustExistAndNotEmpty(scriptPath: String) {
        val resource = ClassPathResource(scriptPath)
        assertThat(resource.exists())
            .`as`("스크립트 파일이 클래스패스에 존재해야 합니다: $scriptPath")
            .isTrue()

        val content = resource.inputStream.bufferedReader().use { it.readText() }
        assertThat(content)
            .`as`("스크립트 파일 내용이 비어있지 않아야 합니다: $scriptPath")
            .isNotBlank()

        assertThat(content)
            .`as`("스크립트 파일에 redis.call 명령어가 포함되어 있어야 합니다: $scriptPath")
            .contains("redis.call")
    }

    @Test
    @DisplayName("RedisLimitedMenuStockReservationManager 클래스 로딩 시 스크립트 로드에 실패하지 않는다")
    fun managerClassLoadingSuccess() {
        val clazz = Class.forName("com.coffee.gu.lock.RedisLimitedMenuStockReservationManager")
        assertThat(clazz).isNotNull
    }

    @Test
    @DisplayName("reserve_stock.lua에서 각 KEYS[1]~KEYS[5]는 올바른 Redis 자료구조 명령어와 바인딩되어야 한다")
    fun reserveStockLuaKeyContract() {
        val content = ClassPathResource("scripts/reserve_stock.lua").inputStream.bufferedReader().use { it.readText() }

        // KEYS[1] = STRING (EXISTS, INCRBY, GET, DECRBY)
        assertThat(content).contains("redis.call('EXISTS', KEYS[1])")
        assertThat(content).contains("redis.call('INCRBY', KEYS[1]")
        assertThat(content).contains("redis.call('GET', KEYS[1])")
        assertThat(content).contains("redis.call('DECRBY', KEYS[1]")

        // KEYS[2] = ZSET (ZRANGE ... BYSCORE, ZREMRANGEBYSCORE, ZADD)
        assertThat(content).contains("redis.call('ZRANGE', KEYS[2], '-inf', ARGV[4], 'BYSCORE')")
        assertThat(content).contains("redis.call('ZREMRANGEBYSCORE', KEYS[2]")
        assertThat(content).contains("redis.call('ZADD', KEYS[2]")

        // KEYS[3] = HASH (HGET, HDEL, HEXISTS, HSET)
        assertThat(content).contains("redis.call('HGET', KEYS[3]")
        assertThat(content).contains("redis.call('HDEL', KEYS[3]")
        assertThat(content).contains("redis.call('HEXISTS', KEYS[3]")
        assertThat(content).contains("redis.call('HSET', KEYS[3]")

        // KEYS[4] = HASH (HGET, HDEL, HSET)
        assertThat(content).contains("redis.call('HGET', KEYS[4]")
        assertThat(content).contains("redis.call('HDEL', KEYS[4]")
        assertThat(content).contains("redis.call('HSET', KEYS[4]")

        // KEYS[5] = SET (SREM, SISMEMBER, SADD)
        assertThat(content).contains("redis.call('SREM', KEYS[5]")
        assertThat(content).contains("redis.call('SISMEMBER', KEYS[5]")
        assertThat(content).contains("redis.call('SADD', KEYS[5]")
    }
}
