package com.coffee.gu.payment

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.PGConfirmResult
import com.coffee.gu.PaymentGatewayConfirm
import com.coffee.gu.lock.LimitedMenuStockReservationManager.ConfirmationResult
import com.coffee.gu.menu.LimitedMenuStockManager
import com.coffee.gu.order.Order
import org.springframework.stereotype.Service

@Service
class PaymentService(
    private val paymentGatewayProcessor: PaymentGatewayProcessor,
    private val paymentPreparer: PaymentPreparer,
    private val paymentCompleter: PaymentCompleter,
    private val paymentManager: PaymentManager,
    private val paymentReader: PaymentReader,
    private val limitedMenuStockManager: LimitedMenuStockManager,
    private val paymentApprovalFailureResolver: PaymentApprovalFailureResolver,
) {
    fun createPayment(order: Order, paymentDiscount: PaymentDiscount): Long {
        return paymentManager.createPayment(order, paymentDiscount)
    }

    fun approvePayment(order: Order): PaymentApprovalResult {
        var payment = paymentReader.getByOrderKey(order.key)
        if (!payment.isReady) return PaymentApprovalResult.fromExisting(payment)
        val pgPayment = paymentGatewayProcessor.getPGPayment(order.key)
        payment = paymentPreparer.prepare(order, pgPayment)
        val menuQuantities = order.toMenuQuantityMap()
        val confirmationResult = limitedMenuStockManager.verifyAndConfirmIfLimited(
            orderKey = order.key,
            menuQuantities = menuQuantities,
        )
        if (confirmationResult == ConfirmationResult.EXPIRED) {
            paymentApprovalFailureResolver.handleExpiredReservation(order, payment, pgPayment)
        }
        val pgConfirmResult: PGConfirmResult = try {
            paymentGatewayProcessor.approvePayment(
                PaymentGatewayConfirm(pgPayment.paymentKey, pgPayment.orderKey, pgPayment.amount)
            )
        } catch (e: Exception) {
            return paymentApprovalFailureResolver.resolveApprovalFailure(order, payment, pgPayment, menuQuantities)
        }
        return try {
            paymentCompleter.complete(order, payment.id, pgConfirmResult)
        } catch (e: CoreException) {
            limitedMenuStockManager.releaseIfLimited(
                orderKey = order.key,
                principalKey = order.principal.key,
                menuQuantities = menuQuantities,
                restoreDbSoldQuantity = false,
            )
            if (e.errorType == ErrorType.LIMITED_MENU_SOLD_OUT) {
                paymentApprovalFailureResolver.autoCancelOversoldPayment(order, payment, pgConfirmResult.paymentKey)
            }
            paymentCompleter.compensateApprovalFailure(
                order,
                payment.id,
                pgConfirmResult.paymentKey,
                e.message ?: "결제 승인 후 내부 완료 처리 실패"
            )
        } catch (e: Exception) {
            limitedMenuStockManager.releaseIfLimited(
                orderKey = order.key,
                principalKey = order.principal.key,
                menuQuantities = menuQuantities,
                restoreDbSoldQuantity = false,
            )
            paymentCompleter.compensateApprovalFailure(
                order,
                payment.id,
                pgConfirmResult.paymentKey,
                e.message ?: "결제 승인 후 내부 완료 처리 실패"
            )
        }
    }
}
