package com.coffee.gu.cancel

import com.coffee.gu.payment.Payment

interface CancelRepository {
    fun cancel(payment: Payment, externalCancelKey: String): Cancel
}
