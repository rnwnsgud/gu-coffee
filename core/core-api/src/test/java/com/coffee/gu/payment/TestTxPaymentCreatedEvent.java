package com.coffee.gu.payment;

import java.util.concurrent.CompletableFuture;

public record TestTxPaymentCreatedEvent(String orderKey, CompletableFuture<Boolean> future) {}
