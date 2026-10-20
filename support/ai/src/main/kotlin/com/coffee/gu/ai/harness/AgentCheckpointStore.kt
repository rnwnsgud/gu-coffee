package com.coffee.gu.ai.harness

import com.coffee.gu.ai.client.LlmMessage
import com.coffee.gu.ai.client.ToolCall
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
