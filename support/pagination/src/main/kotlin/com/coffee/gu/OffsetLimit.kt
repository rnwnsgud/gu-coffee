package com.coffee.gu

class OffsetLimit(
    val offset: Int,
    val limit: Int,
) {
    fun limit(): Int = limit
    fun offset(): Int = offset
    fun withLimit(newLimit: Int): OffsetLimit = OffsetLimit(offset, newLimit)

    companion object {
        private const val MAX_LIMIT = 100
    }

    init {
        if (offset < 0) {
            throw CoreException(ErrorType.INVALID_REQUEST, "offset 은 0 이상이어야 합니다.")
        }
        if (limit <= 0) {
            throw CoreException(ErrorType.INVALID_REQUEST, "limit 은 1 이상이어야 합니다.")
        }
        if (limit > MAX_LIMIT) {
            throw CoreException(ErrorType.INVALID_REQUEST, "limit 은 $MAX_LIMIT 이하여야 합니다.")
        }
    }
}
