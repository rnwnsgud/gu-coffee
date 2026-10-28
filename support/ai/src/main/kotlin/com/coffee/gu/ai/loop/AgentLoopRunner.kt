package com.coffee.gu.ai.loop

import com.coffee.gu.ai.client.LlmClient
import com.coffee.gu.ai.client.LlmMessage
import com.coffee.gu.ai.client.ToolCall
import com.coffee.gu.ai.context.ContextCompactor
import com.coffee.gu.ai.harness.AgentCheckpoint
import com.coffee.gu.ai.harness.AgentCheckpointStore
import com.coffee.gu.ai.harness.InMemoryAgentCheckpointStore
import com.coffee.gu.ai.tool.AgentTool
import org.slf4j.LoggerFactory
import java.util.UUID

class AgentLoopRunner(
    private val llmClient: LlmClient,
    tools: List<AgentTool>,
    private val systemPrompt: String = "당신은 카페 주문/결제 처리를 돕는 백엔드 AI 에이전트입니다.",
    private val maxSteps: Int = 10,
    private val contextCompactor: ContextCompactor = ContextCompactor(),
    private val checkpointStore: AgentCheckpointStore = InMemoryAgentCheckpointStore(),
    private val skipPostApprovalLlmCall: Boolean = false
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val toolMap: Map<String, AgentTool> = tools.associateBy { it.specification().name }
    private val toolSpecs = tools.map { it.specification() }

    enum class Status {
        COMPLETED,
        WAITING_FOR_APPROVAL
    }

    data class AgentExecutionResult(
        val sessionId: String,
        val status: Status,
        val finalAnswer: String,
        val totalSteps: Int,
        val conversationHistory: List<LlmMessage>,
        val pendingApprovalToolCall: ToolCall? = null
    )

    fun run(
        userPrompt: String,
        sessionId: String = "session_${UUID.randomUUID().toString().take(8)}"
    ): AgentExecutionResult {
        val initialMessages = mutableListOf(
            LlmMessage.system(systemPrompt),
            LlmMessage.user(userPrompt)
        )
        return executeLoop(sessionId = sessionId, messages = initialMessages, startStep = 0)
    }

    private fun executeLoop(
        sessionId: String,
        messages: MutableList<LlmMessage>,
        startStep: Int
    ): AgentExecutionResult {
        var step = startStep

        while (step < maxSteps) {
            step++

            // [Phase 3 연동] 매 턴 시작 전 컨텍스트 압축기 호출
            val compacted = contextCompactor.compact(messages.toList())
            messages.clear()
            messages.addAll(compacted)

            // 1. LLM에게 현재까지의 대화 이력(messages)과 가용 도구 명세(toolSpecs)를 전달하여 응답을 받음
            val response = llmClient.chat(messages = messages, tools = toolSpecs)
            messages.add(response)

            // =========================================================================
            // [Mission 1: 루프 종료 조건과 도구 실행 결과 재주입 구현하기]
            //
            // 요구사항:
            // 1) 만약 LLM의 응답(response.toolCalls)이 비어있다면(isEmpty):
            //    더 이상 호출할 도구가 없고 최종 자연어 답변(response.content)이 나왔다는 뜻입니다.
            //    Status.COMPLETED 상태의 AgentExecutionResult를 반환하며 while 루프를 끝내세요.
            //
            // 2) 만약 response.toolCalls에 도구 호출 요청이 들어있다면:
            //    반복문(for)으로 각 toolCall을 순회하면서:
            //    - toolMap에서 toolCall.name으로 실행할 AgentTool을 찾습니다.
            //    - (Mission 4를 할 때는 여기에 HITL 승인 체크 로직이 들어갑니다)
            //    - executeToolSafely(step, tool, toolCall)를 호출해 실행 결과 문자열(observation)을 얻습니다.
            //    - LlmMessage.toolResult(toolCallId = toolCall.id, toolName = toolCall.name, resultJson = observation)를
            //      생성하여 messages 리스트에 추가(add)하세요! (그래야 다음 while 턴에서 LLM이 결과를 볼 수 있습니다)
            // =========================================================================
            if (response.toolCalls.isEmpty()) {
                return AgentExecutionResult(
                    status = Status.COMPLETED,
                    sessionId = sessionId,
                    finalAnswer = response.content.orEmpty(),
                    totalSteps = step,
                    conversationHistory = messages.toList(),
                )
            }
            for (toolCall in response.toolCalls) {
                val tool = toolMap[toolCall.name]
                if(tool != null && tool.requiresApproval()) {
                    checkpointStore.save(AgentCheckpoint(sessionId = sessionId, step = step, messages = messages.toList(), pendingToolCall = toolCall))
                    return AgentExecutionResult(
                        status = Status.WAITING_FOR_APPROVAL,
                        sessionId = sessionId,
                        finalAnswer = "",
                        totalSteps = step,
                        conversationHistory = messages.toList(),
                        pendingApprovalToolCall = toolCall,
                    )
                }
                val observation = executeToolSafely(step, tool, toolCall)
                messages.add(LlmMessage.toolResult(toolCallId = toolCall.id, toolName = toolCall.name, resultJson = observation))
            }
        }

        throw IllegalStateException("최대 허용 루프 횟수(${maxSteps}회)를 초과하여 강제 종료합니다.")
    }

    private fun executeToolSafely(step: Int, tool: AgentTool?, toolCall: ToolCall): String {
        if (tool == null) {
            return "Error: 존재하지 않는 도구입니다 (${toolCall.name})"
        }

        // =========================================================================
        // [Mission 2: Self-Correction(자가 치유)을 위한 예외 피드백 구현하기]
        //
        // 요구사항:
        // tool.execute(toolCall.arguments)를 호출하여 결과를 반환하되,
        // 만약 실행 중 Exception이 발생하면 예외를 밖으로 던져서 프로그램을 죽이지 말고(try-catch),
        // "Error: ${e.message}" 형태의 문자열로 감싸서 반환하세요!
        // (왜냐하면 에러 문구도 하나의 '관찰 결과'로 LLM에게 돌려주어야 LLM이 다음 턴에 파라미터를 고쳐서 재시도할 수 있기 때문입니다)
        // =========================================================================
        return try {
            tool.execute(toolCall.arguments)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun resumeWithApproval(
        sessionId: String,
        approved: Boolean,
        rejectionReason: String = "관리자/사용자가 해당 작업 실행을 거절했습니다."
    ): AgentExecutionResult {
        // =========================================================================
        // [Mission 4-B: Human-in-the-loop 상태 복원 및 루프 재개 구현하기]
        //
        // 요구사항:
        // 1) checkpointStore.findById(sessionId)로 저장된 체크포인트를 꺼냅니다 (없으면 예외 발생).
        // 2) 꺼낸 뒤 checkpointStore.deleteById(sessionId)로 저장소에서 지웁니다.
        // 3) approved == true 이면:
        //    멈춰있던 도구(checkpoint.pendingToolCall)를 executeToolSafely로 실제 실행하여 결과 문자열을 얻습니다.
        //    approved == false 이면:
        //    도구를 실행하지 않고 "User Rejected: $rejectionReason" 문자열을 결과로 만듭니다.
        // 4) 위 결과를 LlmMessage.toolResult(...)로 만들어 checkpoint.messages 복사본(toMutableList)에 추가합니다.
        // 5) executeLoop(sessionId, 복원된_messages, startStep = checkpoint.step)을 호출하여 멈췄던 루프를 마저 끝내세요!
        // =========================================================================
        // 1. 저장소에서 멈췄던 상태 꺼내고 지우기
        val checkpoint = checkpointStore.findById(sessionId)
            ?: throw IllegalArgumentException("체크포인트를 찾을 수 없습니다: $sessionId")
        checkpointStore.deleteById(sessionId)
        val toolCall = checkpoint.pendingToolCall
        val tool = toolMap[toolCall.name]
// 2. 승인(true)이면 아까 멈췄던 도구를 진짜 실행하고, 거절(false)이면 거절 문구 생성
        val observation = if (approved) {
            executeToolSafely(checkpoint.step, tool, toolCall)
        } else {
            "User Rejected: $rejectionReason"
        }
// 3. 아까 멈췄을 때의 대화 기록을 꺼내서, 방금 얻은 도구 결과를 추가
        val messages = checkpoint.messages.toMutableList()
        messages.add(LlmMessage.toolResult(toolCallId = toolCall.id, toolName = toolCall.name, resultJson = observation))

        // [Fast-Path 최적화] 승인/거절 후 굳이 외부 LLM을 다시 호출해 문장을 생성할 필요가 없는 경우 즉시 반환 (35초 -> 0.2초)
        if (skipPostApprovalLlmCall) {
            val fastAnswer = when {
                !approved -> "요청하신 작업(${toolCall.name})이 거절되어 실행되지 않았습니다. (사유: $rejectionReason)"
                observation.startsWith("Error:") -> "승인된 작업 실행 중 오류가 발생했습니다: ${observation.removePrefix("Error:").trim()}"
                else -> {
                    val targetOrderKey = toolCall.arguments["orderKey"] ?: toolCall.arguments["orderId"]
                    if (targetOrderKey != null) {
                        "주문 번호 ${targetOrderKey}의 취소 및 환불 처리가 완료되었습니다."
                    } else {
                        "승인하신 작업(${toolCall.name}) 처리가 완료되었습니다."
                    }
                }
            }
            return AgentExecutionResult(
                sessionId = sessionId,
                status = Status.COMPLETED,
                finalAnswer = fastAnswer,
                totalSteps = checkpoint.step,
                conversationHistory = messages.toList()
            )
        }

// 4. 멈췄던 단계(checkpoint.step)부터 다시 while 루프 이어서 실행!
        return try {
            executeLoop(sessionId = sessionId, messages = messages, startStep = checkpoint.step)
        } catch (e: Exception) {
            // 이미 승인된 도구(예: 주문 취소/환불)가 실행 완료된 상태에서
            // 마지막 답변 요약용 외부 LLM 호출만 일시 장애가 난 경우, 작업 자체는 성공했으므로 안전하게 완료 응답을 반환합니다.
            val fallbackAnswer = if (approved) {
                "승인하신 작업(${toolCall.name}) 처리가 완료되었습니다. (처리 결과: $observation)"
            } else {
                "요청하신 작업(${toolCall.name})이 거절되어 실행되지 않았습니다. (사유: $rejectionReason)"
            }
            AgentExecutionResult(
                sessionId = sessionId,
                status = Status.COMPLETED,
                finalAnswer = fallbackAnswer,
                totalSteps = checkpoint.step,
                conversationHistory = messages.toList()
            )
        }
    }
}
