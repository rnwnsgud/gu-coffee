package com.coffee.gu;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

public record Page<T>(
        List<T> content,
        Boolean hasNext,
        LocalDateTime nextCursor,
        Long nextLastId
) {
    public static <E> Page<E> of(
            List<E> items,
            int pageSize,
            Function<E, LocalDateTime> cursorExtractor,
            Function<E, Long> idExtractor
    ) {
        boolean hasNext = items.size() > pageSize;

        List<E> content = hasNext
                ? items.subList(0, pageSize)
                : items;

        LocalDateTime nextCursor = null;
        Long nextLastId = null;

        if (hasNext && !content.isEmpty()) {
            E lastItem = content.getLast();
            nextCursor = cursorExtractor.apply(lastItem);
            nextLastId = idExtractor.apply(lastItem);
        }

        return new Page<>(content, hasNext, nextCursor, nextLastId);
    }

    public static <E> Page<E> of(List<E> items, int pageSize) {
        boolean hasNext = items.size() > pageSize;

        List<E> content = hasNext
                ? items.subList(0, pageSize)
                : items;

        return new Page<>(content, hasNext, null, null);
    }

    public <R> Page<R> map(Function<T, R> mapper) {
        List<R> mappedContent = this.content.stream().map(mapper).toList();
        return new Page<>(mappedContent, hasNext, nextCursor, nextLastId);
    }
}
