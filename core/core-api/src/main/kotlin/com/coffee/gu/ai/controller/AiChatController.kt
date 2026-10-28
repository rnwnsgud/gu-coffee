package com.coffee.gu.ai.controller

import com.coffee.gu.ai.dto.AiApproveRequest
import com.coffee.gu.ai.dto.AiChatRequest
import com.coffee.gu.ai.dto.AiChatResponse
import com.coffee.gu.ai.loop.AgentLoopRunner
import com.coffee.gu.response.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/ai")
class AiChatController(
    private val agentLoopRunner: AgentLoopRunner
) {

    /**
     * [질문 접수 엔드포인트]
     * 고객의 자연어 질문을 에이전트 루프에 전달하여 즉시 답변을 얻거나,
     * 위험한 작업(취소 등)일 경우 WAITING_FOR_APPROVAL 상태와 승인 대기 도구 정보를 반환합니다.
     */
    @PostMapping("/chat")
    fun chat(
        @RequestBody request: AiChatRequest
    ): ApiResponse<AiChatResponse> {
        val sessionId = request.sessionId ?: "session_${UUID.randomUUID().toString().take(8)}"
        val result = agentLoopRunner.run(
            userPrompt = request.prompt,
            sessionId = sessionId
        )

        val response = AiChatResponse(
            sessionId = result.sessionId,
            status = result.status.name,
            answer = sanitizePlainText(result.finalAnswer),
            pendingToolName = result.pendingApprovalToolCall?.name,
            pendingToolArguments = result.pendingApprovalToolCall?.arguments
        )
        return ApiResponse.success(response)
    }

    /**
     * [HITL 승인/거절 엔드포인트]
     * 일시 정지(WAITING_FOR_APPROVAL)된 세션에 대해 사람의 승인(approved=true) 또는 거절(approved=false)을 전달받아
     * Redis에서 대화 상태를 복원하고 멈췄던 루프를 재개하여 최종 결과를 반환합니다.
     */
    @PostMapping("/approve")
    fun approve(
        @RequestBody request: AiApproveRequest
    ): ApiResponse<AiChatResponse> {
        val result = agentLoopRunner.resumeWithApproval(
            sessionId = request.sessionId,
            approved = request.approved,
            rejectionReason = request.rejectionReason
        )

        val response = AiChatResponse(
            sessionId = result.sessionId,
            status = result.status.name,
            answer = sanitizePlainText(result.finalAnswer),
            pendingToolName = result.pendingApprovalToolCall?.name,
            pendingToolArguments = result.pendingApprovalToolCall?.arguments
        )
        return ApiResponse.success(response)
    }

    /**
     * LLM이 습관적으로 생성하는 마크다운 강조 기호(** 등)와 줄바꿈(\n)을 제거하여
     * Swagger 및 일반 클라이언트에서 깔끔한 단일 평문(Plain Text)으로 표시되도록 정제합니다.
     */
    private fun sanitizePlainText(text: String): String {
        if (text.isBlank()) return text
        return text
            .replace("**", "")
            .replace(Regex("\\s*\\r?\\n+\\s*"), " ")
            .trim()
    }
}
