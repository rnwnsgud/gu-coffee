package com.coffee.gu.ai.tool

import com.coffee.gu.ai.client.ToolSpecification

/**
 * 백엔드 비즈니스 로직(예: OrderService, PaymentService)을
 * 에이전트가 호출할 수 있는 '도구(Tool)'로 노출하기 위한 인터페이스입니다.
 */
interface AgentTool {
    /**
     * LLM에게 전달할 도구의 이름, 설명, JSON Schema 명세
     */
    fun specification(): ToolSpecification

    /**
     * LLM이 반환한 JSON 인자(arguments)를 받아 실제 백엔드 로직을 실행하고 결과를 문자열(JSON 등)로 반환
     */
    fun execute(arguments: Map<String, Any?>): String

    /**
     * [Phase 4: Human-in-the-loop]
     * true일 경우 에이전트가 이 도구를 마음대로 즉시 실행하지 못하고,
     * 루프를 일시 정지(Suspend)한 뒤 사람의 명시적 승인을 기다립니다. (예: 주문 취소, 환불, DB 삭제)
     */
    fun requiresApproval(): Boolean = false
}
