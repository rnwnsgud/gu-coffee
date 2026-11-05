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
        val cancelFailed = try {
            paymentGatewayProcessor.cancelPayment(
                PaymentGatewayCancel(paymentKey, ErrorType.LIMITED_MENU_OVERSOLD_AUTO_CANCELED.message)
            )
            false
        } catch (cancelEx: Exception) {
            true
        }

        if (cancelFailed) {
            paymentCompleter.compensateApprovalFailure(
                order,
                payment.id,
                paymentKey,
                ErrorType.LIMITED_MENU_OVERSOLD_AUTO_CANCELED.message,
            )
        } else {
            paymentCompleter.failProcess(
                order,
                payment,
                ErrorType.LIMITED_MENU_OVERSOLD_AUTO_CANCELED.code.name,
                ErrorType.LIMITED_MENU_OVERSOLD_AUTO_CANCELED.message,
            )
        }
        throw CoreException(ErrorType.LIMITED_MENU_OVERSOLD_AUTO_CANCELED)
    }

    fun resolveApprovalFailure(
        order: Order,
        payment: Payment,
        pgPayment: PGPayment,
        menuQuantities: Map<Long, Int>,
    ): PaymentApprovalResult {
        return try {
            val currentPg = paymentGatewayProcessor.getPGPayment(order.key)
            if (currentPg.status == PaymentGatewayStatus.DONE) {
                val recoveredResult = PGConfirmResult.success(
                    currentPg.orderKey,
                    currentPg.paymentKey,
                    payment.method,
                    payment.approveCode,
                    payment.paidAt
                )
                paymentCompleter.complete(order, payment.id, recoveredResult)
            } else {
                limitedMenuStockManager.releaseIfLimited(
                    orderKey = order.key,
                    principalKey = order.principal.key,
                    menuQuantities = menuQuantities,
                    restoreDbSoldQuantity = false,
                )
                paymentCompleter.failProcess(order, payment, "PG_APPROVE_TIMEOUT", "승인 타임아웃 후 미승인 확인")
                PaymentApprovalResult.failed(order.key, payment.externalPaymentKey ?: "", OffsetDateTime.now())
            }
        } catch (inquiryEx: Exception) {
            val cancelFailed = try {
                paymentGatewayProcessor.cancelPayment(
                    PaymentGatewayCancel(pgPayment.paymentKey, "승인 응답 및 상태 조회 타임아웃에 따른 자동 망취소")
                )
                false
            } catch (cancelEx: Exception) {
                true
            }

            limitedMenuStockManager.releaseIfLimited(
                orderKey = order.key,
                principalKey = order.principal.key,
                menuQuantities = menuQuantities,
                restoreDbSoldQuantity = false,
            )

            if (cancelFailed) {
                paymentCompleter.compensateApprovalFailure(
                    order,
                    payment.id,
                    pgPayment.paymentKey,
                    "승인 및 상태 조회 네트워크 타임아웃 (자동 망취소 실패)"
                )
            } else {
                paymentCompleter.failProcess(order, payment, "NETWORK_TIMEOUT", "승인 및 상태 조회 네트워크 타임아웃")
                PaymentApprovalResult.failed(order.key, payment.externalPaymentKey ?: "", OffsetDateTime.now())
            }
        }
    }
}
