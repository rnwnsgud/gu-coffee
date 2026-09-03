package com.coffee.gu

interface PaymentGateway {
    fun provider(): PaymentGatewayProvider
    fun confirm(confirm: PaymentGatewayConfirm): PGConfirmResult
    fun getByOrderKey(orderKey: String): PGPayment
    fun cancel(cancel: PaymentGatewayCancel): PGCancelResult
}
