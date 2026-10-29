package com.coffee.gu.ai.client

/**
 * 토스페이먼츠 연동의 PaymentGateway 인터페이스처럼,
 * 특정 LLM 벤더(Gemini, OpenAI 등)에 종속되지 않도록 추상화한 LLM 호출 인터페이스입니다.
 */
interface LlmClient {
    fun chat(
        messages: List<LlmMessage>,
        tools: List<ToolSpecification> = emptyList()
    ): LlmMessage
}
