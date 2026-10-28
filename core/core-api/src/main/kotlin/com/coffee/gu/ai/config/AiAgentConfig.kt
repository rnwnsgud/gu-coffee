package com.coffee.gu.ai.config

import com.coffee.gu.ai.client.LlmClient
import com.coffee.gu.ai.context.ContextCompactor
import com.coffee.gu.ai.harness.AgentCheckpointStore
import com.coffee.gu.ai.harness.RedisAgentCheckpointStore
import com.coffee.gu.ai.loop.AgentLoopRunner
import com.coffee.gu.ai.tool.AgentTool
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Configuration
class AiAgentConfig {

    @Bean
    fun agentCheckpointStore(
        redisTemplate: StringRedisTemplate,
        objectMapper: ObjectMapper
    ): AgentCheckpointStore {
        return RedisAgentCheckpointStore(
            redisTemplate = redisTemplate,
            objectMapper = objectMapper,
            ttl = Duration.ofMinutes(30)
        )
    }

    @Bean
    fun agentLoopRunner(
        llmClient: LlmClient,
        tools: List<AgentTool>,
        checkpointStore: AgentCheckpointStore
    ): AgentLoopRunner {
        return AgentLoopRunner(
            llmClient = llmClient,
            tools = tools,
            systemPrompt = """
                당신은 Gu-Coffee 카페의 주문 및 결제 처리를 돕는 AI 고객지원 에이전트입니다.
                1. 고객이 주문 취소를 요청하면 사전 상태 조회(getOrderStatus)를 먼저 호출하지 말고 즉시 cancelOrder 도구를 호출하세요(취소 사유가 없으면 '고객 요청'으로 입력).
                2. 고객이 단순 주문 상태 조회만 요청하거나 상태 확인 후 취소를 명시적으로 요청한 경우에만 getOrderStatus를 호출하세요.
                3. 최종 답변 작성 시 마크다운 기호(**, *, # 등)와 줄바꿈(\n)을 절대 사용하지 말고, 한두 문장의 깔끔한 평문(Plain Text)으로만 답변하세요.
            """.trimIndent(),
            contextCompactor = ContextCompactor(),
            checkpointStore = checkpointStore,
            skipPostApprovalLlmCall = true
        )
    }
}
