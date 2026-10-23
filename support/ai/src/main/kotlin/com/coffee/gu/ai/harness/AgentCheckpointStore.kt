package com.coffee.gu.ai.harness

import com.coffee.gu.ai.client.LlmMessage
import com.coffee.gu.ai.client.ToolCall
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * [Phase 4: 하네스 & Human-in-the-loop (HITL)]
 *
 * 에이전트가 위험한 도구(예: cancelOrder)를 호출하려 할 때,
 * 백엔드 스레드를 블로킹(sleep)한 채로 사람의 승인을 기다리면 서버 스레드 풀이 고갈됩니다.
 *
 * 따라서 현재까지의 대화 컨텍스트(messages)와 대기 중인 도구 호출(pendingToolCall)을
 * 체크포인트 저장소(실무에서는 Redis나 RDBMS)에 직렬화해 저장하고 루프를 즉시 종료한 뒤,
 * 나중에 사람이 승인/거절 API를 호출하면 저장된 체크포인트를 꺼내어 루프를 이어서 실행(Resume)합니다.
 */
data class AgentCheckpoint(
    val sessionId: String,
    val step: Int,
    val messages: List<LlmMessage>,
    val pendingToolCall: ToolCall
)

interface AgentCheckpointStore {
    fun save(checkpoint: AgentCheckpoint)
    fun findById(sessionId: String): AgentCheckpoint?
    fun deleteById(sessionId: String)
}

class InMemoryAgentCheckpointStore : AgentCheckpointStore {
    private val store = ConcurrentHashMap<String, AgentCheckpoint>()

    override fun save(checkpoint: AgentCheckpoint) {
        store[checkpoint.sessionId] = checkpoint
    }

    override fun findById(sessionId: String): AgentCheckpoint? = store[sessionId]

    override fun deleteById(sessionId: String) {
        store.remove(sessionId)
    }
}

/**
 * [Phase 4 분산 하네스: Redis 기반 분산 체크포인트 저장소]
 *
 * 서버가 여러 대로 로드밸런싱되거나 재배포되더라도,
 * 세션 정보(messages, pendingToolCall)를 Redis에 안전하게 보관하여 다른 서버에서도 루프를 재개할 수 있습니다.
 * TTL(기본 30분)을 두어 승인되지 않고 방치된 세션의 메모리 누수를 자동 방지합니다.
 */
class RedisAgentCheckpointStore(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val ttl: Duration = Duration.ofMinutes(30),
    private val keyPrefix: String = "ai:checkpoint:"
) : AgentCheckpointStore {

    override fun save(checkpoint: AgentCheckpoint) {
        val key = keyPrefix + checkpoint.sessionId
        val json = objectMapper.writeValueAsString(checkpoint)
        redisTemplate.opsForValue().set(key, json, ttl)
    }

    override fun findById(sessionId: String): AgentCheckpoint? {
        val key = keyPrefix + sessionId
        val json = redisTemplate.opsForValue().get(key) ?: return null
        return try {
            objectMapper.readValue<AgentCheckpoint>(json)
        } catch (e: Exception) {
            null
        }
    }

    override fun deleteById(sessionId: String) {
        val key = keyPrefix + sessionId
        redisTemplate.delete(key)
    }
}
