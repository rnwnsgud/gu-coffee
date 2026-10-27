package com.coffee.gu.ai.controller

import com.coffee.gu.ai.client.ToolCall
import com.coffee.gu.ai.dto.AiApproveRequest
import com.coffee.gu.ai.dto.AiChatRequest
import com.coffee.gu.ai.loop.AgentLoopRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper

class AiChatControllerTest {

    private val agentLoopRunner: AgentLoopRunner = mock()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val controller = AiChatController(agentLoopRunner)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat - 위험한 작업(취소 등) 요청 시 WAITING_FOR_APPROVAL 상태와 대기 도구 정보가 반환된다")
    fun chat_returnsWaitingForApproval() {
        val request = AiChatRequest(prompt = "ORD-101 주문 취소해줘", sessionId = "sess_test_1")
        val executionResult = AgentLoopRunner.AgentExecutionResult(
            sessionId = "sess_test_1",
            status = AgentLoopRunner.Status.WAITING_FOR_APPROVAL,
            finalAnswer = "",
            totalSteps = 2,
            conversationHistory = emptyList(),
            pendingApprovalToolCall = ToolCall(
                id = "call_abc",
                name = "cancelOrder",
                arguments = mapOf("orderKey" to "ORD-101", "reason" to "단순 변심")
            )
        )

        whenever(agentLoopRunner.run(eq("ORD-101 주문 취소해줘"), eq("sess_test_1"))).thenReturn(executionResult)

        mockMvc.perform(
            post("/api/v1/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.sessionId").value("sess_test_1"))
            .andExpect(jsonPath("$.data.status").value("WAITING_FOR_APPROVAL"))
            .andExpect(jsonPath("$.data.pendingToolName").value("cancelOrder"))
            .andExpect(jsonPath("$.data.pendingToolArguments.orderKey").value("ORD-101"))
    }

    @Test
    @DisplayName("POST /api/v1/ai/approve - 사람이 승인(approved=true)을 전달하면 루프가 재개되어 최종 완료 답변이 반환된다")
    fun approve_resumesAndReturnsCompleted() {
        val request = AiApproveRequest(
            sessionId = "sess_test_1",
            approved = true
        )
        val executionResult = AgentLoopRunner.AgentExecutionResult(
            sessionId = "sess_test_1",
            status = AgentLoopRunner.Status.COMPLETED,
            finalAnswer = "ORD-101 주문 취소 및 환불 처리가 완료되었습니다.",
            totalSteps = 3,
            conversationHistory = emptyList(),
            pendingApprovalToolCall = null
        )

        whenever(agentLoopRunner.resumeWithApproval(eq("sess_test_1"), eq(true), any())).thenReturn(executionResult)

        mockMvc.perform(
            post("/api/v1/ai/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.sessionId").value("sess_test_1"))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.answer").value("ORD-101 주문 취소 및 환불 처리가 완료되었습니다."))
    }
}
