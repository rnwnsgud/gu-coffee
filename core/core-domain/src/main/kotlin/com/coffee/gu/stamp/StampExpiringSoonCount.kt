package com.coffee.gu.stamp

@JvmRecord
data class StampExpiringSoonCount(
    val principalKey: String,
    val count: Int,
)
