package com.coffee.gu.api.controller.v1.request;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.store.StoreSearch;


public record StoreSearchRequest(
        Double latitude,
        Double longitude,
        Double radiusKm
) {
    public StoreSearch toStoreSearch() {
        if (latitude == null || longitude == null || radiusKm == null) {
            throw new CoreException(ErrorType.INVALID_REQUEST, null);
        }
        return new StoreSearch(latitude, longitude, radiusKm);
    }
}
