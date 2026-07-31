package com.coffee.gu.response;

import java.time.LocalDateTime;
import java.util.List;

public record PageResponse<T>(
        List<T> content,
        Boolean hasNext,
        LocalDateTime nextCursor,
        Long nextLastId
) {
}
