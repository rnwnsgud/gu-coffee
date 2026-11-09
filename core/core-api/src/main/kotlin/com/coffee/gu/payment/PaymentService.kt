package com.coffee.gu.payment

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
            principalKey = order.principal.key,
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
        } catch (e: Exception) {
            paymentApprovalFailureResolver.resolveCompletionFailure(
                order = order,
                payment = payment,
                paymentKey = pgConfirmResult.paymentKey,
                menuQuantities = menuQuantities,
                exception = e,
            )
        }
    }
}
