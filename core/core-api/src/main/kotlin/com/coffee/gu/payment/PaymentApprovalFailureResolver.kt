package com.coffee.gu.payment

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.PGConfirmResult
import com.coffee.gu.PGPayment
import com.coffee.gu.PaymentGatewayCancel
import com.coffee.gu.PaymentGatewayStatus
import com.coffee.gu.menu.LimitedMenuStockManager
import com.coffee.gu.order.Order
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class PaymentApprovalFailureResolver(
    private val paymentGatewayProcessor: PaymentGatewayProcessor,
    private val paymentCompleter: PaymentCompleter,
    private val limitedMenuStockManager: LimitedMenuStockManager,
) {

    fun handleExpiredReservation(
        order: Order,
        payment: Payment,
        pgPayment: PGPayment,
    ): Nothing {
        if (pgPayment.status == PaymentGatewayStatus.DONE) {
            autoCancelOversoldPayment(order, payment, pgPayment.paymentKey)
        }
        paymentCompleter.failProcess(
            order,
            payment,
            ErrorType.LIMITED_MENU_RESERVATION_EXPIRED.code.name,
            ErrorType.LIMITED_MENU_RESERVATION_EXPIRED.message,
        )
        throw CoreException(ErrorType.LIMITED_MENU_RESERVATION_EXPIRED)
    }

    fun autoCancelOversoldPayment(
        order: Order,
        payment: Payment,
        paymentKey: String,
    ): Nothing {
        cancelOrCompensate(
            order = order,
            payment = payment,
            paymentKey = paymentKey,
            cancelReason = ErrorType.LIMITED_MENU_OVERSOLD_AUTO_CANCELED.message,
            failCode = ErrorType.LIMITED_MENU_OVERSOLD_AUTO_CANCELED.code.name,
        )
        throw CoreException(ErrorType.LIMITED_MENU_OVERSOLD_AUTO_CANCELED)
    }

    fun resolveApprovalFailure(
        order: Order,
        payment: Payment,
        pgPayment: PGPayment,
        menuQuantities: Map<Long, Int>,
    ): PaymentApprovalResult {
        val currentPg = try {
            paymentGatewayProcessor.getPGPayment(order.key)
        } catch (inquiryEx: Exception) {
            return handleInquiryFailure(order, payment, pgPayment, menuQuantities)
        }

        return if (currentPg.status == PaymentGatewayStatus.DONE) {
            handleApprovedRecovery(order, payment, currentPg, menuQuantities)
        } else {
            handleUnapprovedFailure(order, payment, menuQuantities)
        }
    }

    fun resolveCompletionFailure(
        order: Order,
        payment: Payment,
        paymentKey: String,
        menuQuantities: Map<Long, Int>,
        exception: Exception,
    ): PaymentApprovalResult {
        limitedMenuStockManager.releaseIfLimited(
            orderKey = order.key,
            principalKey = order.principal.key,
            menuQuantities = menuQuantities,
            restoreDbSoldQuantity = false,
        )
        if (exception is CoreException && exception.errorType == ErrorType.LIMITED_MENU_SOLD_OUT) {
            autoCancelOversoldPayment(order, payment, paymentKey)
        } else {
            val reason = exception.message ?: "결제 승인 후 내부 완료 처리 실패"
            return cancelOrCompensate(
                order = order,
                payment = payment,
                paymentKey = paymentKey,
                cancelReason = reason,
                failCode = "COMPLETION_FAILED",
                failMessage = reason,
                compensateReason = "$reason (자동 취소 실패)",
            )
        }
    }

    private fun handleInquiryFailure(
        order: Order,
        payment: Payment,
        pgPayment: PGPayment,
        menuQuantities: Map<Long, Int>,
    ): PaymentApprovalResult {
        limitedMenuStockManager.releaseIfLimited(
            orderKey = order.key,
            principalKey = order.principal.key,
            menuQuantities = menuQuantities,
            restoreDbSoldQuantity = false,
        )
        return cancelOrCompensate(
            order = order,
            payment = payment,
            paymentKey = pgPayment.paymentKey,
            cancelReason = "승인 응답 및 상태 조회 타임아웃에 따른 자동 망취소",
            failCode = "NETWORK_TIMEOUT",
            failMessage = "승인 및 상태 조회 네트워크 타임아웃",
            compensateReason = "승인 및 상태 조회 네트워크 타임아웃 (자동 망취소 실패)",
        )
    }

    private fun handleApprovedRecovery(
        order: Order,
        payment: Payment,
        currentPg: PGPayment,
        menuQuantities: Map<Long, Int>,
    ): PaymentApprovalResult {
        val recoveredResult = PGConfirmResult.success(
            currentPg.orderKey,
            currentPg.paymentKey,
            payment.method,
            payment.approveCode,
            payment.paidAt
        )
        return try {
            paymentCompleter.complete(order, payment.id, recoveredResult)
        } catch (e: Exception) {
            resolveCompletionFailure(
                order = order,
                payment = payment,
                paymentKey = currentPg.paymentKey,
                menuQuantities = menuQuantities,
                exception = e,
            )
        }
    }

    private fun handleUnapprovedFailure(
        order: Order,
        payment: Payment,
        menuQuantities: Map<Long, Int>,
    ): PaymentApprovalResult {
        limitedMenuStockManager.releaseIfLimited(
            orderKey = order.key,
            principalKey = order.principal.key,
            menuQuantities = menuQuantities,
            restoreDbSoldQuantity = false,
        )
        paymentCompleter.failProcess(order, payment, "PG_APPROVE_TIMEOUT", "승인 타임아웃 후 미승인 확인")
        return PaymentApprovalResult.failed(order.key, payment.externalPaymentKey ?: "", OffsetDateTime.now())
    }

    private fun cancelOrCompensate(
        order: Order,
        payment: Payment,
        paymentKey: String,
        cancelReason: String,
        failCode: String,
        failMessage: String = cancelReason,
        compensateReason: String = cancelReason,
    ): PaymentApprovalResult {
        val cancelFailed = try {
            paymentGatewayProcessor.cancelPayment(
                PaymentGatewayCancel(paymentKey, cancelReason)
            )
            false
        } catch (cancelEx: Exception) {
            true
        }

        return if (cancelFailed) {
            paymentCompleter.compensateApprovalFailure(
                order,
                payment.id,
                paymentKey,
                compensateReason,
            )
        } else {
            paymentCompleter.failProcess(order, payment, failCode, failMessage)
            PaymentApprovalResult.failed(order.key, payment.externalPaymentKey ?: paymentKey, OffsetDateTime.now())
        }
    }
}
