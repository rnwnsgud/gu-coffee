package com.coffee.gu.payment;

import java.util.concurrent.CompletableFuture;

public record TestPaymentCreatedEvent(String orderKey, CompletableFuture<Boolean> future) {}
