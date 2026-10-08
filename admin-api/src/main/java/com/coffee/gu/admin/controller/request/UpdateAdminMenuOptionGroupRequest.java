package com.coffee.gu.admin.controller.request;

import java.util.List;

public record UpdateAdminMenuOptionGroupRequest(
        Long menuId,
        List<Long> optionGroupIds
) {
    public UpdateAdminMenuOptionGroupRequest {
        if (optionGroupIds == null) {
            optionGroupIds = List.of();
        }
    }

    public UpdateAdminMenuOptionGroupRequest(List<Long> optionGroupIds) {
        this(null, optionGroupIds);
    }
}
