package com.coffee.gu

import java.time.LocalDateTime

data class Page<T>(
    val content: List<T>,
    val hasNext: Boolean,
    val nextCursor: LocalDateTime? = null,
    val nextLastId: Long? = null,
) {

    fun <R> map(transform: (T) -> R): Page<R> {
        val mappedContent = this.content.map(transform)
        return Page(mappedContent, hasNext, nextCursor, nextLastId)
    }

    companion object {
        fun <E> of(
            items: List<E>,
            pageSize: Int,
            cursorExtractor: ((E) -> LocalDateTime)? = null,
            idExtractor: ((E) -> Long)? = null,
        ): Page<E> {
            val hasNext = items.size > pageSize
            val content = if (hasNext) items.subList(0, pageSize) else items

            var nextCursor: LocalDateTime? = null
            var nextLastId: Long? = null

            if (hasNext && content.isNotEmpty() && cursorExtractor != null && idExtractor != null) {
                val lastItem = content.last()
                nextCursor = cursorExtractor(lastItem)
                nextLastId = idExtractor(lastItem)
            }

            return Page(content, hasNext, nextCursor, nextLastId)
        }
    }
}