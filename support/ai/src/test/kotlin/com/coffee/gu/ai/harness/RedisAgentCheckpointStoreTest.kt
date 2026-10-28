package com.coffee.gu.ai.harness

import com.coffee.gu.ai.client.LlmMessage
import com.coffee.gu.ai.client.ToolCall
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration

class RedisAgentCheckpointStoreTest {

    private val redisTemplate: StringRedisTemplate = mock()
    private val valueOperations: ValueOperations<String, String> = mock()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val store = RedisAgentCheckpointStore(
        redisTemplate = redisTemplate,
        objectMapper = objectMapper,
        ttl = Duration.ofMinutes(30)
    )

    init {
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
    }

    @Test
    @DisplayName("save는 세션 ID를 키로 하여 JSON 직렬화된 체크포인트를 TTL과 함께 Redis에 저장한다")
    fun save_serializesAndSavesWithTtl() {
        val checkpoint = AgentCheckpoint(
            sessionId = "sess_101",
            step = 2,
            messages = listOf(
                LlmMessage.user("주문 취소해줘"),
                LlmMessage.assistantToolCalls(
                    listOf(ToolCall(id = "c1", name = "cancelOrder", arguments = mapOf("orderId" to 101)))
                )
            ),
            pendingToolCall = ToolCall(id = "c1", name = "cancelOrder", arguments = mapOf("orderId" to 101))
        )

        store.save(checkpoint)

        verify(valueOperations).set(
            eq("ai:checkpoint:sess_101"),
            any<String>(),
            eq(Duration.ofMinutes(30))
        )
    }

    @Test
    @DisplayName("findById는 Redis의 JSON 문자열을 AgentCheckpoint 객체로 정상 역직렬화한다")
    fun findById_deserializesFromJson() {
        val originalCheckpoint = AgentCheckpoint(
            sessionId = "sess_101",
            step = 2,
            messages = listOf(LlmMessage.user("주문 취소해줘")),
            pendingToolCall = ToolCall(id = "c1", name = "cancelOrder", arguments = mapOf("orderId" to 101))
        )
        val json = objectMapper.writeValueAsString(originalCheckpoint)

        whenever(valueOperations.get("ai:checkpoint:sess_101")).thenReturn(json)

        val found = store.findById("sess_101")

        assertThat(found).isNotNull
        assertThat(found?.sessionId).isEqualTo("sess_101")
        assertThat(found?.step).isEqualTo(2)
        assertThat(found?.pendingToolCall?.name).isEqualTo("cancelOrder")
        assertThat(found?.messages).hasSize(1)
    }

    @Test
    @DisplayName("deleteById는 세션 키를 Redis에서 제거한다")
    fun deleteById_deletesKey() {
        store.deleteById("sess_101")

        verify(redisTemplate).delete("ai:checkpoint:sess_101")
    }
}
