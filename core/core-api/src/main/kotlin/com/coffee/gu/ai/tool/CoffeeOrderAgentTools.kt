package com.coffee.gu.ai.tool

import com.coffee.gu.ai.client.ToolSpecification
import com.coffee.gu.cancel.CancelService
import com.coffee.gu.order.OrderReader
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * 기존 커피 주문 도메인의 OrderReader를 AI 에이전트 도구로 감싸는 어댑터입니다.
 * 에이전트는 이를 통해 사내 DB의 주문 상태를 실시간 조회합니다.
 */
@Component
class GetOrderStatusAgentTool(
    private val orderReader: OrderReader
) : AgentTool {
    private val objectMapper = jacksonObjectMapper()

    override fun specification(): ToolSpecification {
        return ToolSpecification(
            name = "getOrderStatus",
            description = "주문 번호(orderKey)로 현재 주문 상태, 상품명, 결제 금액을 조회합니다.",
            parametersSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "orderKey" to mapOf("type" to "string", "description" to "조회할 주문 고유 번호(예: ORD-101)")
                ),
                "required" to listOf("orderKey")
            )
        )
    }

    override fun execute(arguments: Map<String, Any?>): String {
        val orderKey = arguments["orderKey"]?.toString()
            ?: throw IllegalArgumentException("주문 번호(orderKey) 파라미터가 누락되었습니다.")

        val order = orderReader.getByOrderKey(orderKey)
        val result = mapOf(
            "orderKey" to order.key,
            "orderName" to order.name,
            "totalPrice" to order.totalPrice,
            "state" to order.state.name
        )
        return objectMapper.writeValueAsString(result)
    }
}

/**
 * 기존 CancelService를 감싸는 주문 취소 도구 어댑터입니다.
 * [Phase 4: Human-in-the-loop]
 * 결제 취소 및 환불을 유발하므로 requiresApproval() = true로 선언하여
 * 사람의 결재가 떨어지기 전에는 절대 마음대로 실행되지 않습니다.
 */
@Component
class CancelOrderAgentTool(
    private val orderReader: OrderReader,
    private val cancelService: CancelService
) : AgentTool {
    private val objectMapper = jacksonObjectMapper()

    override fun specification(): ToolSpecification {
        return ToolSpecification(
            name = "cancelOrder",
            description = "주문을 취소하고 결제를 환불합니다. 반드시 주문 상태가 PAID일 때만 취소 가능합니다.",
            parametersSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "orderKey" to mapOf("type" to "string", "description" to "취소할 주문 번호(orderKey)"),
                    "reason" to mapOf("type" to "string", "description" to "주문 취소 사유")
                ),
                "required" to listOf("orderKey", "reason")
            )
        )
    }

    override fun requiresApproval(): Boolean = true

    override fun execute(arguments: Map<String, Any?>): String {
        val orderKey = arguments["orderKey"]?.toString()
            ?: throw IllegalArgumentException("주문 번호(orderKey) 파라미터가 누락되었습니다.")

        val order = orderReader.getByOrderKey(orderKey)
        cancelService.cancel(order)

        val result = mapOf(
            "orderKey" to order.key,
            "status" to "CANCELED",
            "message" to "주문 취소 및 환불 처리가 완료되었습니다."
        )
        return objectMapper.writeValueAsString(result)
    }
}
