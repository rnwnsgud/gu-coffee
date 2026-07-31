package com.coffee.gu.store;

public record StoreSearch(
        Double latitude,
        Double longitude,
        Double radiusKm
) {
}
