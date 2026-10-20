package com.coffee.gu.ai

import com.coffee.gu.ai.client.GeminiRestClient
import com.coffee.gu.ai.client.LlmClient
import com.coffee.gu.ai.client.LlmMessage
import com.coffee.gu.ai.client.ToolCall
import com.coffee.gu.ai.client.ToolSpecification
import com.coffee.gu.ai.context.ContextCompactor
import com.coffee.gu.ai.loop.AgentLoopRunner
import com.coffee.gu.ai.tool.AgentTool
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class AgentFromScratchLabTest {

    @Test
    @DisplayName("[Phase 1] GeminiRestClient는 도구 명세(JSON Schema)를 전송하고 LLM의 functionCall JSON 응답을 ToolCall로 파싱한다")
    fun phase1_geminiRestClient_parsesFunctionCallJson() {
        val builder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(builder).build()

        val geminiClient = GeminiRestClient(
            restClient = builder.build(),
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            model = "gemini-2.5-flash",
            apiKey = "test-api-key"
        )

        val fakeGeminiJsonResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "role": "model",
                    "parts": [
                      {
                        "functionCall": {
                          "name": "getOrderStatus",
                          "args": {
                            "orderId": 101
                          }
                        }
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=test-api-key"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(fakeGeminiJsonResponse, MediaType.APPLICATION_JSON))

        val toolSpec = ToolSpecification(
            name = "getOrderStatus",
            description = "주문 번호로 현재 주문 상태를 조회합니다.",
            parametersSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "orderId" to mapOf("type" to "integer", "description" to "주문 ID")
                ),
                "required" to listOf("orderId")
            )
        )

        val response = geminiClient.chat(
            messages = listOf(LlmMessage.user("101번 주문 상태 알려줘")),
            tools = listOf(toolSpec)
        )

        assertThat(response.role).isEqualTo(LlmMessage.Role.ASSISTANT)
        assertThat(response.toolCalls).hasSize(1)
        assertThat(response.toolCalls.first().name).isEqualTo("getOrderStatus")
        assertThat(response.toolCalls.first().arguments["orderId"]).isEqualTo(101)
        mockServer.verify()
    }

    @Test
    @DisplayName("[Phase 2] 에이전트 루프는 여러 도구를 연속 호출하며 에러 발생 시 스스로 파라미터를 교정한다")
    fun phase2_agentLoop_multiStepAndSelfCorrection() {
        val getOrderTool = object : AgentTool {
            override fun specification() = ToolSpecification(
                name = "getOrderStatus",
                description = "주문 상태를 조회합니다.",
                parametersSchema = mapOf("type" to "object")
            )

            override fun execute(arguments: Map<String, Any?>): String {
                val orderId = arguments["orderId"]
                return """{"orderId": $orderId, "menu": "아이스 아메리카노", "status": "PREPARING"}"""
            }
        }

        val cancelOrderTool = object : AgentTool {
            override fun specification() = ToolSpecification(
                name = "cancelOrder",
                description = "주문을 취소합니다. 반드시 취소 사유(reason)를 함께 전달해야 합니다.",
                parametersSchema = mapOf("type" to "object")
            )

            override fun execute(arguments: Map<String, Any?>): String {
                val orderId = arguments["orderId"]
                val reason = arguments["reason"] as? String
                if (reason.isNullOrBlank()) {
                    throw IllegalArgumentException("취소 사유(reason) 파라미터가 누락되었습니다. 사유를 포함해 다시 호출하세요.")
                }
                return """{"orderId": $orderId, "status": "CANCELED", "cancelReason": "$reason"}"""
            }
        }

        val scriptedLlm = object : LlmClient {
            private var turn = 0

            override fun chat(messages: List<LlmMessage>, tools: List<ToolSpecification>): LlmMessage {
                turn++
                return when (turn) {
                    1 -> LlmMessage.assistantToolCalls(
                        listOf(ToolCall(id = "call_1", name = "getOrderStatus", arguments = mapOf("orderId" to 101)))
                    )
                    2 -> LlmMessage.assistantToolCalls(
                        listOf(ToolCall(id = "call_2", name = "cancelOrder", arguments = mapOf("orderId" to 101)))
                    )
                    3 -> {
                        val lastToolResult = messages.last().content.orEmpty()
                        assertThat(lastToolResult).contains("Error: 취소 사유(reason) 파라미터가 누락되었습니다")
                        LlmMessage.assistantToolCalls(
                            listOf(
                                ToolCall(
                                    id = "call_3",
                                    name = "cancelOrder",
                                    arguments = mapOf("orderId" to 101, "reason" to "고객 요청에 의한 취소")
                                )
                            )
                        )
                    }
                    else -> LlmMessage.assistant("101번 주문(아이스 아메리카노)이 아직 상품 준비 중이어서 정상적으로 취소 처리했습니다.")
                }
            }
        }

        val runner = AgentLoopRunner(
            llmClient = scriptedLlm,
            tools = listOf(getOrderTool, cancelOrderTool)
        )

        val result = runner.run("내 101번 주문 확인해서 아직 준비 중이면 취소해 줘.")

        assertThat(result.status).isEqualTo(AgentLoopRunner.Status.COMPLETED)
        assertThat(result.totalSteps).isEqualTo(4)
        assertThat(result.finalAnswer).contains("정상적으로 취소 처리했습니다")
        assertThat(result.conversationHistory).hasSize(9)
    }

    @Test
    @DisplayName("[Phase 3] 컨텍스트 엔지니어링: 오래된 대용량 도구 결과(JSON)는 다음 턴에 자동으로 요약 마스킹되어 토큰 낭비를 막는다")
    fun phase3_contextCompactor_masksOldLargeToolObservations() {
        // 1,000자가 넘는 거대한 DB 조회 JSON을 반환하는 도구
        val heavyMenuDumpTool = object : AgentTool {
            override fun specification() = ToolSpecification(
                name = "getAllMenus",
                description = "전체 메뉴 상세 JSON을 조회합니다.",
                parametersSchema = mapOf("type" to "object")
            )

            override fun execute(arguments: Map<String, Any?>): String {
                val hugeJson = (1..50).joinToString(", ") {
                    """{"menuId": $it, "name": "스페셜 원두 커피 $it", "description": "매우 긴 설명 텍스트가 들어있는 상세 필드"}"""
                }
                return "[$hugeJson]"
            }
        }

        val orderCheckTool = object : AgentTool {
            override fun specification() = ToolSpecification(
                name = "getOrderStatus",
                description = "주문 상태를 조회합니다.",
                parametersSchema = mapOf("type" to "object")
            )

            override fun execute(arguments: Map<String, Any?>): String {
                return """{"orderId": 101, "status": "PREPARING"}"""
            }
        }

        val scriptedLlm = object : LlmClient {
            private var turn = 0

            override fun chat(messages: List<LlmMessage>, tools: List<ToolSpecification>): LlmMessage {
                turn++
                return when (turn) {
                    // 1턴: 거대한 메뉴 목록 조회 호출 (약 3,000자 반환됨)
                    1 -> LlmMessage.assistantToolCalls(
                        listOf(ToolCall(id = "call_1", name = "getAllMenus", arguments = emptyMap()))
                    )
                    // 2턴: 이제 두 번째 도구(주문 상태 조회) 호출
                    2 -> {
                        // 직전 턴(가장 최근 도구 결과 1개)이므로 아직은 3,000자 원본이 그대로 살아있음
                        val firstToolMsg = messages.first { it.role == LlmMessage.Role.TOOL }
                        assertThat(firstToolMsg.content!!.length).isGreaterThan(1000)

                        LlmMessage.assistantToolCalls(
                            listOf(ToolCall(id = "call_2", name = "getOrderStatus", arguments = mapOf("orderId" to 101)))
                        )
                    }
                    // 3턴: 두 번째 도구 결과까지 쌓인 시점!
                    // 이제 1턴의 거대했던 3,000자짜리 메뉴 JSON은 '오래된 도구 결과'가 되어 100자 내외로 마스킹되었는지 검증!
                    else -> {
                        val toolMessages = messages.filter { it.role == LlmMessage.Role.TOOL }
                        val oldMenuToolMsg = toolMessages[0]
                        val recentOrderToolMsg = toolMessages[1]

                        assertThat(oldMenuToolMsg.content).contains("[과거 도구('getAllMenus') 결과 압축됨")
                        assertThat(oldMenuToolMsg.content!!.length).isLessThan(150) // 3,000자 -> 120자로 압축!
                        assertThat(recentOrderToolMsg.content).isEqualTo("""{"orderId": 101, "status": "PREPARING"}""")

                        LlmMessage.assistant("메뉴와 주문 상태를 모두 확인했습니다.")
                    }
                }
            }
        }

        val runner = AgentLoopRunner(
            llmClient = scriptedLlm,
            tools = listOf(heavyMenuDumpTool, orderCheckTool),
            contextCompactor = ContextCompactor(maxRawToolResultLength = 200, keepRecentToolResultsCount = 1)
        )

        val result = runner.run("메뉴 목록이랑 내 101번 주문 확인해 줘.")
        assertThat(result.status).isEqualTo(AgentLoopRunner.Status.COMPLETED)
    }

    @Test
    @DisplayName("[Phase 4] 하네스 & HITL: requiresApproval=true인 주문 취소 도구는 실행 직전에 멈추며(WAITING_FOR_APPROVAL), 사람이 승인하면 이어서 실행된다")
    fun phase4_humanInTheLoop_pauseAndResumeOnApproval() {
        var isCancelExecutedInDb = false

        // 1. 안전한 조회 도구: requiresApproval = false (기본값) -> 에이전트가 즉시 실행 가능
        val getOrderTool = object : AgentTool {
            override fun specification() = ToolSpecification(
                name = "getOrderStatus",
                description = "주문 상태를 조회합니다.",
                parametersSchema = mapOf("type" to "object")
            )

            override fun execute(arguments: Map<String, Any?>): String {
                return """{"orderId": 101, "status": "PREPARING", "amount": 4500}"""
            }
        }

        // 2. 위험한 상태 변경 도구: requiresApproval = true -> 반드시 사람의 결재를 받아야 함!
        val cancelOrderWithHitlTool = object : AgentTool {
            override fun specification() = ToolSpecification(
                name = "cancelOrder",
                description = "주문을 취소하고 환불합니다.",
                parametersSchema = mapOf("type" to "object")
            )

            override fun requiresApproval(): Boolean = true

            override fun execute(arguments: Map<String, Any?>): String {
                isCancelExecutedInDb = true
                return """{"orderId": ${arguments["orderId"]}, "status": "CANCELED", "refundedAmount": 4500}"""
            }
        }

        val scriptedLlm = object : LlmClient {
            private var turn = 0

            override fun chat(messages: List<LlmMessage>, tools: List<ToolSpecification>): LlmMessage {
                turn++
                return when (turn) {
                    1 -> LlmMessage.assistantToolCalls(
                        listOf(ToolCall(id = "call_1", name = "getOrderStatus", arguments = mapOf("orderId" to 101)))
                    )
                    2 -> LlmMessage.assistantToolCalls(
                        listOf(
                            ToolCall(
                                id = "call_2",
                                name = "cancelOrder",
                                arguments = mapOf("orderId" to 101, "reason" to "고객 요청")
                            )
                        )
                    )
                    else -> LlmMessage.assistant("고객님의 승인이 확인되어 101번 주문 취소 및 4,500원 환불이 완료되었습니다.")
                }
            }
        }

        val runner = AgentLoopRunner(
            llmClient = scriptedLlm,
            tools = listOf(getOrderTool, cancelOrderWithHitlTool)
        )

        // [1차 실행] 사용자가 주문 취소를 요청함
        val pausedResult = runner.run(userPrompt = "101번 주문 취소해 줘.", sessionId = "order_cs_session_1")

        // 검증 1: getOrderStatus는 자동 실행되었지만, cancelOrder 직전에 하네스가 루프를 일시 정지시킴!
        assertThat(pausedResult.status).isEqualTo(AgentLoopRunner.Status.WAITING_FOR_APPROVAL)
        assertThat(pausedResult.pendingApprovalToolCall?.name).isEqualTo("cancelOrder")
        assertThat(pausedResult.pendingApprovalToolCall?.arguments).containsEntry("orderId", 101)
        // 아직 실제 DB 취소 로직은 절대 실행되지 않았음!
        assertThat(isCancelExecutedInDb).isFalse()

        // [2차 실행: 사람의 승인 버튼 클릭!] 저장된 sessionId로 승인(approved = true) 전달하여 루프 재개
        val resumedResult = runner.resumeWithApproval(
            sessionId = "order_cs_session_1",
            approved = true
        )

        // 검증 2: 멈췄던 Step 2부터 이어서 cancelOrder가 실제 실행되고 최종 답변까지 도달함!
        assertThat(isCancelExecutedInDb).isTrue()
        assertThat(resumedResult.status).isEqualTo(AgentLoopRunner.Status.COMPLETED)
        assertThat(resumedResult.finalAnswer).contains("4,500원 환불이 완료되었습니다")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    @DisplayName("[Live 연동] 실제 Google Gemini API를 호출하여 Phase 1~4 전체 과정을 라이브로 실행한다")
    fun live_realGeminiApi_endToEnd() {
        val apiKey = System.getenv("GEMINI_API_KEY")
        val realGeminiClient = GeminiRestClient(
            restClient = RestClient.create(),
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            model = "gemini-2.5-flash",
            apiKey = apiKey
        )

        val getOrderTool = object : AgentTool {
            override fun specification() = ToolSpecification(
                name = "getOrderStatus",
                description = "주문 번호(orderId)로 주문 상태와 메뉴를 조회합니다.",
                parametersSchema = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "orderId" to mapOf("type" to "integer", "description" to "조회할 주문 번호")
                    ),
                    "required" to listOf("orderId")
                )
            )

            override fun execute(arguments: Map<String, Any?>): String {
                val orderId = (arguments["orderId"] as Number).toInt()
                return """{"orderId": $orderId, "menu": "바닐라 라떼", "status": "PREPARING", "amount": 5500}"""
            }
        }

        val cancelOrderTool = object : AgentTool {
            override fun specification() = ToolSpecification(
                name = "cancelOrder",
                description = "주문을 취소합니다. 반드시 주문 상태가 PREPARING일 때만 호출해야 하며, 취소 사유(reason)를 입력해야 합니다.",
                parametersSchema = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "orderId" to mapOf("type" to "integer", "description" to "취소할 주문 번호"),
                        "reason" to mapOf("type" to "string", "description" to "취소 사유")
                    ),
                    "required" to listOf("orderId", "reason")
                )
            )

            override fun requiresApproval(): Boolean = true

            override fun execute(arguments: Map<String, Any?>): String {
                val orderId = (arguments["orderId"] as Number).toInt()
                val reason = arguments["reason"]
                return """{"orderId": $orderId, "status": "CANCELED", "reason": "$reason", "refundedAmount": 5500}"""
            }
        }

        val runner = AgentLoopRunner(
            llmClient = realGeminiClient,
            tools = listOf(getOrderTool, cancelOrderTool)
        )

        // 1. 실제 Gemini에게 요청 -> 스스로 getOrderStatus(101) 호출 후 cancelOrder(101) 호출 시점에 HITL 일시 정지!
        val step1Result = runner.run(
            userPrompt = "주문 번호 101번 상태를 먼저 조회해 보고, 아직 준비 중(PREPARING)이면 '단순 변심' 사유로 주문을 취소해 줘.",
            sessionId = "live_session_101"
        )

        println("=== [1차 실행 결과 (HITL 일시 정지)] ===")
        println("Status: ${step1Result.status}")
        println("승인 대기 중인 도구: ${step1Result.pendingApprovalToolCall}")
        assertThat(step1Result.status).isEqualTo(AgentLoopRunner.Status.WAITING_FOR_APPROVAL)

        // 2. 승인 후 재개 -> 실제 Gemini가 취소 결과를 받아 최종 한국어 답변 생성!
        val finalResult = runner.resumeWithApproval(
            sessionId = "live_session_101",
            approved = true
        )

        println("=== [2차 승인 후 최종 결과] ===")
        println("Final Answer: ${finalResult.finalAnswer}")
        assertThat(finalResult.status).isEqualTo(AgentLoopRunner.Status.COMPLETED)
    }
}
