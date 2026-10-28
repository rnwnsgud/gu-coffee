package com.coffee.gu.ai.client

/**
 * LLM과 백엔드 서버가 주고받는 단일 메시지 단위입니다.
 * LLM은 무상태(Stateless)이므로 매 호출마다 지금까지의 List<LlmMessage> 전체를 전송해야 합니다.
 */
data class LlmMessage(
    val role: Role,
    val content: String? = null,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null,
    val toolName: String? = null
) {
    enum class Role {
        SYSTEM,
        USER,
        ASSISTANT,
        TOOL
    }

    companion object {
        fun system(content: String) = LlmMessage(role = Role.SYSTEM, content = content)
        fun user(content: String) = LlmMessage(role = Role.USER, content = content)
        fun assistant(content: String) = LlmMessage(role = Role.ASSISTANT, content = content)
        fun assistantToolCalls(toolCalls: List<ToolCall>) = LlmMessage(role = Role.ASSISTANT, toolCalls = toolCalls)
        fun toolResult(toolCallId: String, toolName: String, resultJson: String) = LlmMessage(
            role = Role.TOOL,
            content = resultJson,
            toolCallId = toolCallId,
            toolName = toolName
        )
    }
}

/**
 * LLM이 "이 백엔드 함수를 이 인자(arguments)로 대신 실행해 달라"고 반환하는 요청 객체입니다.
 */
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any?>,
    val thoughtSignature: String? = null
)

/**
 * 백엔드가 LLM에게 "현재 네가 사용할 수 있는 도구 목록과 파라미터 스키마(JSON Schema)야"라고 알려주는 명세입니다.
 */
data class ToolSpecification(
    val name: String,
    val description: String,
    val parametersSchema: Map<String, Any>
)
