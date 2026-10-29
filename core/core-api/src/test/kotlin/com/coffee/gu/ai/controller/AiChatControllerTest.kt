package com.coffee.gu.ai.controller

import com.coffee.gu.Principal
import com.coffee.gu.ai.client.GeminiRestClient
import com.coffee.gu.ai.client.ToolCall
import com.coffee.gu.ai.dto.AiApproveRequest
import com.coffee.gu.ai.dto.AiChatRequest
import com.coffee.gu.ai.loop.AgentLoopRunner
import com.coffee.gu.ai.tool.CancelOrderAgentTool
import com.coffee.gu.ai.tool.GetOrderStatusAgentTool
import com.coffee.gu.api.controller.RestDocsTest
import com.coffee.gu.cancel.CancelService
import com.coffee.gu.enums.OrderState
import com.coffee.gu.order.Order
import com.coffee.gu.order.OrderReader
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.JsonNodeType
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal

class AiChatControllerTest : RestDocsTest() {

    private val agentLoopRunner: AgentLoopRunner = mock()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    override val controller: Any
        get() = AiChatController(agentLoopRunner)

    @Test
    @DisplayName("POST /v1/ai/chat - 위험한 작업(취소 등) 요청 시 WAITING_FOR_APPROVAL 상태와 대기 도구 정보가 반환된다")
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
            post("/v1/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.sessionId").value("sess_test_1"))
            .andExpect(jsonPath("$.data.status").value("WAITING_FOR_APPROVAL"))
            .andExpect(jsonPath("$.data.pendingToolName").value("cancelOrder"))
            .andExpect(jsonPath("$.data.pendingToolArguments.orderKey").value("ORD-101"))
            .andDo(
                document(
                    "ai-chat",
                    requestFields(
                        fieldWithPath("prompt").description("고객 자연어 질문 (예: 'ORD-101 주문 취소해줘')"),
                        fieldWithPath("sessionId").description("세션 식별자 (생략 시 자동 생성)").optional().type(JsonNodeType.STRING)
                    ),
                    responseFields(
                        fieldWithPath("status").description("응답 상태"),
                        fieldWithPath("data.sessionId").description("에이전트 대화 세션 ID"),
                        fieldWithPath("data.status").description("에이전트 루프 상태 (COMPLETED / WAITING_FOR_APPROVAL / MAX_STEPS_EXCEEDED)"),
                        fieldWithPath("data.answer").description("에이전트 최종 답변 (승인 대기 중일 때는 빈 문자열)"),
                        fieldWithPath("data.pendingToolName").description("사람의 승인을 대기 중인 도구 이름").optional().type(JsonNodeType.STRING),
                        subsectionWithPath("data.pendingToolArguments").description("승인 대기 중인 도구 호출 인자 Map").optional().type(JsonNodeType.OBJECT),
                        fieldWithPath("error").description("에러 정보 (정상 시 null)")
                    )
                )
            )
    }

    @Test
    @DisplayName("POST /v1/ai/approve - 사람이 승인(approved=true)을 전달하면 루프가 재개되어 최종 완료 답변이 반환된다")
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
            post("/v1/ai/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.sessionId").value("sess_test_1"))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.answer").value("ORD-101 주문 취소 및 환불 처리가 완료되었습니다."))
            .andDo(
                document(
                    "ai-approve",
                    requestFields(
                        fieldWithPath("sessionId").description("승인 대기 중인 세션 ID"),
                        fieldWithPath("approved").description("승인 여부 (true: 승인 및 실행, false: 거절)"),
                        fieldWithPath("rejectionReason").description("거절 시 사유 메시지").optional().type(JsonNodeType.STRING)
                    ),
                    responseFields(
                        fieldWithPath("status").description("응답 상태"),
                        fieldWithPath("data.sessionId").description("에이전트 대화 세션 ID"),
                        fieldWithPath("data.status").description("에이전트 루프 상태 (COMPLETED)"),
                        fieldWithPath("data.answer").description("에이전트 최종 답변"),
                        fieldWithPath("data.pendingToolName").description("승인 대기 도구 이름 (완료 시 null)").optional().type(JsonNodeType.STRING),
                        subsectionWithPath("data.pendingToolArguments").description("승인 대기 도구 인자 (완료 시 null)").optional().type(JsonNodeType.OBJECT),
                        fieldWithPath("error").description("에러 정보 (정상 시 null)")
                    )
                )
            )
    }

    @Test
    @DisplayName("[실전 API 통합 테스트] Controller -> AgentLoopRunner -> CoffeeOrderTools -> GeminiRestClient 전 구간을 관통하여 검증한다")
    fun live_fullApiPipeline_withRealGemini() {
        val apiKey = resolveGeminiApiKey()
        val isLiveKeyProvided = apiKey.isNotBlank() && apiKey != "dummy-key"

        val orderReader: OrderReader = mock()
        val cancelService: CancelService = mock()

        val sampleOrder = Order(
            key = "ORD-101",
            name = "아이스 카페라떼",
            principal = Principal.user("U1"),
            storeId = 1L,
            totalPrice = BigDecimal.valueOf(5000),
            state = OrderState.PAID,
            lines = emptyList()
        )
        whenever(orderReader.getByOrderKey("ORD-101")).thenReturn(sampleOrder)

        val restClientBuilder = RestClient.builder()
        val baseUrl = "https://generativelanguage.googleapis.com/v1beta"
        val model = "gemini-3.5-flash"

        if (!isLiveKeyProvided) {
            println("💡 [안내] 실제 Gemini API Key가 설정되지 않아 Mock Gemini HTTP 서버 모드로 전 구간 통합 테스트를 실행합니다.")
            println("💡 실제 구글 서버와 통신하려면 application.yml의 gemini.api.key 또는 GEMINI_API_KEY 환경변수에 키를 넣어주세요.")
            bindMockGeminiResponses(restClientBuilder, baseUrl, model, apiKey)
        } else {
            println("🚀 [Live 모드] 실제 Google Gemini API 서버와 실시간 통신하여 테스트를 실행합니다!")
        }

        // Mock이 아닌 진짜 GeminiRestClient + 진짜 AgentLoopRunner + 진짜 사내 Tool 어댑터 조립!
        val realGeminiClient = GeminiRestClient(
            restClient = restClientBuilder.build(),
            baseUrl = baseUrl,
            model = model,
            apiKey = apiKey
        )
        val realRunner = AgentLoopRunner(
            llmClient = realGeminiClient,
            tools = listOf(
                GetOrderStatusAgentTool(orderReader),
                CancelOrderAgentTool(orderReader, cancelService)
            )
        )
        val liveMockMvc = MockMvcBuilders.standaloneSetup(AiChatController(realRunner)).build()

        // 1. 고객이 POST /v1/ai/chat 호출 -> WAITING_FOR_APPROVAL 응답이 내려오고 아직 cancelService는 호출되지 않음!
        val chatRequest = AiChatRequest(
            prompt = "주문번호 ORD-101 상태를 먼저 확인하고, 결제 완료(PAID) 상태라면 '단순 변심'으로 주문 취소해 줘.",
            sessionId = "live_api_sess_101"
        )

        liveMockMvc.perform(
            post("/v1/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatRequest))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("WAITING_FOR_APPROVAL"))
            .andExpect(jsonPath("$.data.pendingToolName").value("cancelOrder"))
            .andExpect(jsonPath("$.data.pendingToolArguments.orderKey").value("ORD-101"))

        // 아직 사람이 승인 버튼을 안 눌렀으므로 실제 취소 서비스는 절대 호출되지 않았어야 함!
        verify(cancelService, never()).cancel(any())

        // 2. 고객이 화면에서 [승인] 버튼을 눌러 POST /v1/ai/approve 호출!
        val approveRequest = AiApproveRequest(
            sessionId = "live_api_sess_101",
            approved = true
        )

        liveMockMvc.perform(
            post("/v1/ai/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approveRequest))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))

        // 승인 후 실제로 사내 CancelService.cancel(sampleOrder)가 정확히 1번 호출되었음을 검증!
        verify(cancelService).cancel(sampleOrder)
    }

    /**
     * 1순위: OS 환경변수 (GEMINI_API_KEY)
     * 2순위: src/test/resources/application.yml 의 gemini.api.key 값
     */
    private fun resolveGeminiApiKey(): String {
        val envKey = System.getenv("GEMINI_API_KEY")
        if (!envKey.isNullOrBlank()) {
            return envKey
        }
        val yamlFactory = YamlPropertiesFactoryBean()
        yamlFactory.setResources(ClassPathResource("application.yml"))
        val rawValue = yamlFactory.`object`?.getProperty("gemini.api.key")?.trim().orEmpty()
        return if (rawValue.startsWith("\${") && rawValue.endsWith("}")) {
            rawValue.substringAfter(":", "dummy-key").removeSuffix("}")
        } else {
            rawValue.ifBlank { "dummy-key" }
        }
    }

    private fun bindMockGeminiResponses(
        builder: RestClient.Builder,
        baseUrl: String,
        model: String,
        apiKey: String
    ) {
        val mockServer = MockRestServiceServer.bindTo(builder).build()
        val endpoint = "$baseUrl/models/$model:generateContent?key=$apiKey"

        // 1턴 응답: getOrderStatus("ORD-101") 호출
        mockServer.expect(requestTo(endpoint))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """
                    {
                      "candidates": [{
                        "content": {
                          "role": "model",
                          "parts": [{
                            "functionCall": { "name": "getOrderStatus", "args": { "orderKey": "ORD-101" } },
                            "thoughtSignature": "sig_1"
                          }]
                        }
                      }]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        // 2턴 응답: cancelOrder("ORD-101", "단순 변심") 호출 -> 여기서 HITL 인터셉트 발동!
        mockServer.expect(requestTo(endpoint))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """
                    {
                      "candidates": [{
                        "content": {
                          "role": "model",
                          "parts": [{
                            "functionCall": { "name": "cancelOrder", "args": { "orderKey": "ORD-101", "reason": "단순 변심" } },
                            "thoughtSignature": "sig_2"
                          }]
                        }
                      }]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        // 3턴 응답 (승인 후): 최종 취소 완료 답변
        mockServer.expect(requestTo(endpoint))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """
                    {
                      "candidates": [{
                        "content": {
                          "role": "model",
                          "parts": [{
                            "text": "주문번호 ORD-101(아이스 카페라떼, 5,000원)이 결제 완료 상태임을 확인하여 '단순 변심' 사유로 주문 취소 및 환불을 완료했습니다."
                          }]
                        }
                      }]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )
    }
}
