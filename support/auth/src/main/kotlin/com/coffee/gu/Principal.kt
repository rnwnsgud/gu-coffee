package com.coffee.gu

import com.coffee.gu.enums.PrincipalType

data class Principal(
    val key: String,
    val type: PrincipalType,
) {
    init {
        if (key.isBlank()) {
            throw CoreException(ErrorType.INVALID_REQUEST, null)
        }
    }

    constructor(key: String, typeName: String) : this(
        key = key,
        type = PrincipalType.valueOf(typeName),
    )

    companion object {
        @JvmStatic
        fun user(userId: String): Principal = Principal("U$userId", PrincipalType.USER)

        @JvmStatic
        fun guest(guestKey: String): Principal = Principal("G$guestKey", PrincipalType.GUEST)
    }
}
