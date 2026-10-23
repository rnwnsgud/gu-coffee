package com.coffee.gu.ai.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
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
    @param:Value("\${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private val baseUrl: String,
    @param:Value("\${gemini.api.model:gemini-3.5-flash}")
    private val model: String,
    @param:Value("\${gemini.api.key:dummy-key}")
    private val apiKey: String
) : LlmClient {

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

        val response = restClient.post()
            .uri("$baseUrl/models/$model:generateContent?key=$apiKey")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body<GeminiGenerateContentResponse>()
            ?: throw IllegalStateException("Gemini API 응답이 비어 있습니다.")

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
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null,
    val functionCall: GeminiFunctionCall? = null,
    val functionResponse: GeminiFunctionResponse? = null,
    val thoughtSignature: String? = null
)

data class GeminiFunctionCall(
    val name: String,
    val args: Map<String, Any?> = emptyMap()
)

data class GeminiFunctionResponse(
    val name: String,
    val response: Map<String, Any?>
)

data class GeminiTool(
    val functionDeclarations: List<GeminiFunctionDeclaration>
)

data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
)

data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

data class GeminiCandidate(
    val content: GeminiContent? = null
)
