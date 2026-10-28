package com.coffee.gu.ai.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.http.HttpClient
import java.time.Duration
import java.util.UUID

/**
 * 외부 AI 프레임워크(Spring AI, LangChain 등) 없이
 * 순수 Spring RestClient만으로 Google Gemini API와 통신하는 클라이언트입니다.
 *
 * 기존 TossPaymentsGateway와 완벽히 동일한 HTTP POST + JSON 직렬화 원리로 동작합니다.
 */
@Component
class GeminiRestClient(
    private val restClient: RestClient,
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String
) : LlmClient {

    /**
     * Spring 컨테이너가 빈을 생성할 때 사용하는 생성자입니다.
     * PG용 짧은 타임아웃(Read 5초)이 걸린 전역 RestClient 빈과 섞이지 않도록,
     * LLM 추론 및 Thinking 시간을 고려한 전용 RestClient(Read 60초)를 생성하여 사용합니다.
     */
    @Autowired
    constructor(
        @Value("\${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta}")
        baseUrl: String,
        @Value("\${gemini.api.model:gemini-3.5-flash-lite}")
        model: String,
        @Value("\${gemini.api.key:dummy-key}")
        apiKey: String
    ) : this(
        restClient = createDefaultLlmRestClient(),
        baseUrl = baseUrl,
        model = model,
        apiKey = apiKey
    )

    companion object {
        private val log = LoggerFactory.getLogger(GeminiRestClient::class.java)
        private const val FALLBACK_MODEL = "gemini-3.5-flash-lite"

        private fun createDefaultLlmRestClient(): RestClient {
            val httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build()
            val factory = JdkClientHttpRequestFactory(httpClient).apply {
                setReadTimeout(Duration.ofSeconds(60))
            }
            return RestClient.builder()
                .requestFactory(factory)
                .build()
        }
    }

    override fun chat(
        messages: List<LlmMessage>,
        tools: List<ToolSpecification>
    ): LlmMessage {
        val systemInstruction = messages
            .firstOrNull { it.role == LlmMessage.Role.SYSTEM }
            ?.content
            ?.let { GeminiContent(parts = listOf(GeminiPart(text = it))) }

        val contents = messages
            .filter { it.role != LlmMessage.Role.SYSTEM }
            .map { msg -> toGeminiContent(msg) }

        val geminiTools = if (tools.isNotEmpty()) {
            listOf(
                GeminiTool(
                    functionDeclarations = tools.map { tool ->
                        GeminiFunctionDeclaration(
                            name = tool.name,
                            description = tool.description,
                            parameters = tool.parametersSchema
                        )
                    }
                )
            )
        } else {
            null
        }

        val request = GeminiGenerateContentRequest(
            systemInstruction = systemInstruction,
            contents = contents,
            tools = geminiTools
        )

        val response = callGenerateContentWithFallback(request)

        val parts = response.candidates.firstOrNull()?.content?.parts.orEmpty()
        val toolCalls = parts.mapNotNull { part ->
            part.functionCall?.let { fc ->
                ToolCall(
                    id = "call_${UUID.randomUUID().toString().take(8)}",
                    name = fc.name,
                    arguments = fc.args,
                    thoughtSignature = part.thoughtSignature
                )
            }
        }

        if (toolCalls.isNotEmpty()) {
            return LlmMessage.assistantToolCalls(toolCalls)
        }

        val textResponse = parts.mapNotNull { it.text }.joinToString("\n")
        return LlmMessage.assistant(textResponse)
    }

    /**
     * Google Gemini 서버의 일시적 트래픽 과부하(503 Service Unavailable 등) 발생 시
     * 짧은 대기 후 재시도하고, 계속 5xx 발생 시 경량 모델(gemini-3.5-flash-lite)로 자동 폴백(Fallback)합니다.
     */
    private fun callGenerateContentWithFallback(
        request: GeminiGenerateContentRequest
    ): GeminiGenerateContentResponse {
        val candidateModels = if (model == FALLBACK_MODEL) {
            listOf(model, model)
        } else {
            listOf(model, FALLBACK_MODEL)
        }

        var lastException: Exception? = null
        for ((attempt, targetModel) in candidateModels.withIndex()) {
            val startNs = System.nanoTime()
            try {
                val res = restClient.post()
                    .uri("$baseUrl/models/$targetModel:generateContent?key=$apiKey")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body<GeminiGenerateContentResponse>()
                    ?: throw IllegalStateException("Gemini API 응답이 비어 있습니다.")

                val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
                log.info(
                    "[Gemini API 응답 완료] model={}, 소요시간={}ms, promptTokens={}, thoughtsTokens(내부추론)={}, outputTokens={}",
                    targetModel,
                    elapsedMs,
                    res.usageMetadata?.promptTokenCount ?: 0,
                    res.usageMetadata?.thoughtsTokenCount ?: 0,
                    res.usageMetadata?.candidatesTokenCount ?: 0
                )
                return res
            } catch (e: HttpServerErrorException) {
                val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
                lastException = e
                val nextModel = candidateModels.getOrNull(attempt + 1)
                if (nextModel != null) {
                    log.warn(
                        "[Gemini API 5xx 과부하] model={}, status={}, 대기소요={}ms -> 즉시 폴백 수행(nextModel={})",
                        targetModel,
                        e.statusCode,
                        elapsedMs,
                        nextModel
                    )
                }
            }
        }
        throw lastException ?: IllegalStateException("Gemini API 호출에 실패했습니다.")
    }

    private fun toGeminiContent(msg: LlmMessage): GeminiContent {
        return when (msg.role) {
            LlmMessage.Role.USER -> GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = msg.content))
            )
            LlmMessage.Role.ASSISTANT -> {
                if (msg.toolCalls.isNotEmpty()) {
                    GeminiContent(
                        role = "model",
                        parts = msg.toolCalls.map { tc ->
                            GeminiPart(
                                functionCall = GeminiFunctionCall(name = tc.name, args = tc.arguments),
                                thoughtSignature = tc.thoughtSignature
                            )
                        }
                    )
                } else {
                    GeminiContent(
                        role = "model",
                        parts = listOf(GeminiPart(text = msg.content))
                    )
                }
            }
            LlmMessage.Role.TOOL -> GeminiContent(
                role = "user",
                parts = listOf(
                    GeminiPart(
                        functionResponse = GeminiFunctionResponse(
                            name = msg.toolName ?: "unknown_tool",
                            response = mapOf("result" to (msg.content ?: ""))
                        )
                    )
                )
            )
            LlmMessage.Role.SYSTEM -> throw IllegalArgumentException("SYSTEM role is handled via systemInstruction")
        }
    }
}

// --- Google Gemini REST API DTOs ---

data class GeminiGenerateContentRequest(
    val systemInstruction: GeminiContent? = null,
    val contents: List<GeminiContent>,
    val tools: List<GeminiTool>? = null
)

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart> = emptyList()
)

data class GeminiPart(
    val text: String? = null,
    val functionCall: GeminiFunctionCall? = null,
    val functionResponse: GeminiFunctionResponse? = null,
    val thoughtSignature: String? = null
)

data class GeminiFunctionCall(
    val name: String = "",
    val args: Map<String, Any?> = emptyMap()
)

data class GeminiFunctionResponse(
    val name: String = "",
    val response: Map<String, Any?> = emptyMap()
)

data class GeminiTool(
    val functionDeclarations: List<GeminiFunctionDeclaration> = emptyList()
)

data class GeminiFunctionDeclaration(
    val name: String = "",
    val description: String = "",
    val parameters: Map<String, Any> = emptyMap()
)

data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val usageMetadata: GeminiUsageMetadata? = null
)

data class GeminiUsageMetadata(
    val promptTokenCount: Int = 0,
    val candidatesTokenCount: Int = 0,
    val thoughtsTokenCount: Int = 0,
    val totalTokenCount: Int = 0
)

data class GeminiCandidate(
    val content: GeminiContent? = null
)
