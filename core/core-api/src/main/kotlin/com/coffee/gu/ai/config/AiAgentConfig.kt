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
                고객이 주문 상태를 묻거나 취소를 요청하면 가용한 도구를 사용하여 확인 후 정확하게 안내하세요.
                주문 취소 시에는 반드시 사전에 상태가 PAID(결제완료)인지 확인해야 합니다.
            """.trimIndent(),
            contextCompactor = ContextCompactor(),
            checkpointStore = checkpointStore
        )
    }
}
