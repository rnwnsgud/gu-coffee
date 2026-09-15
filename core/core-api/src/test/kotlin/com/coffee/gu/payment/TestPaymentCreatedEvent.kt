package com.coffee.gu.payment

import java.util.concurrent.CompletableFuture

data class TestPaymentCreatedEvent(
    val orderKey: String,
    val future: CompletableFuture<Boolean>
)
