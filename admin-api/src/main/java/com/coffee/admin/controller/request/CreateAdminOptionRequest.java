package com.coffee.admin.controller.request;

public record CreateAdminOptionRequest(
        Long optionGroupId,
        String name,
        long extraPrice
) {
    public CreateAdminOptionRequest {
        if (extraPrice < 0) {
            throw new IllegalArgumentException("Extra price cannot be negative.");
        }
    }
}
