package com.coffee.gu.response

import java.time.LocalDateTime

class PageResponse<T>(
    val content: List<T>,
    val hasNext: Boolean,
    val nextCursor: LocalDateTime? = null,
    val nextLastId: Long? = null
)
