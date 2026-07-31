package com.coffee.gu;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record OffsetLimit(
        Integer offset,
        Integer limit
) implements Pageable {

    private static final int MAX_LIMIT = 100;

    public OffsetLimit {
        if (offset == null || offset < 0) {
            throw new CoreException(ErrorType.INVALID_REQUEST, "offset 은 0 이상이어야 합니다.");
        }
        if (limit == null || limit <= 0) {
            throw new CoreException(ErrorType.INVALID_REQUEST, "limit 은 1 이상이어야 합니다.");
        }
        if (limit > MAX_LIMIT) {
            throw new CoreException(ErrorType.INVALID_REQUEST, "limit 은" + MAX_LIMIT + " 이하여야 합니다.");
        }
    }

    @Override
    public int getPageNumber() {
        return offset / limit;
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return Sort.unsorted();
    }

    @Override
    public Pageable next() {
        return new OffsetLimit(Math.addExact(offset, limit), limit);
    }

    @Override
    public Pageable previousOrFirst() {
        return new OffsetLimit(Math.max(0, offset - limit), limit);
    }

    @Override
    public Pageable first() {
        return new OffsetLimit(0, limit);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        if (pageNumber < 0) {
            throw new CoreException(ErrorType.INVALID_REQUEST, "pageNumber 은 0 이상이어야 합니다.");
        }
        return new OffsetLimit(Math.multiplyExact(pageNumber, limit), limit);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }

    public OffsetLimit withLimit(int newLimit) {
        return new OffsetLimit(offset, newLimit);
    }
}
