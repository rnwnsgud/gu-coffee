package com.coffee.gu.admin.controller.request;

public record CreateAdminOptionGroupRequest(
        String name,
        Boolean isExclusive,
        Boolean isRequired
) {
}
