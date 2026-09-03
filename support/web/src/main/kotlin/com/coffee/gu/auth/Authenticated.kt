package com.coffee.gu.auth

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Authenticated(
    val required: Boolean = true
)
