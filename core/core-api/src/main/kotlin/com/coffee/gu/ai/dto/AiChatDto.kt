package com.coffee.gu.ai.dto

data class AiChatRequest(
    val prompt: String,
    val sessionId: String? = null
)

data class AiChatResponse(
    val sessionId: String,
    val status: String,
    val answer: String,
    val pendingToolName: String? = null,
    val pendingToolArguments: Map<String, Any?>? = null
)

data class AiApproveRequest(
    val sessionId: String,
    val approved: Boolean,
    val rejectionReason: String = "사용자가 해당 작업 실행을 거절했습니다."
)
