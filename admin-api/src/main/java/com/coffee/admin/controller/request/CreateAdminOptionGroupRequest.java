package com.coffee.admin.controller.request;

public record CreateAdminOptionGroupRequest(
        String name,
        Boolean isExclusive,
        Boolean isRequired
) {
}
