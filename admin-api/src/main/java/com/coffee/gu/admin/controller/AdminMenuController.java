package com.coffee.gu.admin.controller;

import com.coffee.gu.admin.controller.request.CreateAdminMenuOptionGroup;
import com.coffee.gu.admin.controller.request.CreateAdminOptionGroupRequest;
import com.coffee.gu.admin.controller.request.CreateAdminOptionRequest;
import com.coffee.gu.admin.domain.AdminMenuService;
import com.coffee.gu.admin.controller.request.CreateAdminMenuRequest;
import com.coffee.gu.admin.support.response.ApiResponse;
import com.coffee.gu.admin.controller.request.UpdateAdminMenuOptionGroupRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/menu")
public class AdminMenuController {

    private final AdminMenuService adminMenuService;

    public AdminMenuController(AdminMenuService adminMenuService) {
        this.adminMenuService = adminMenuService;
    }

    @PostMapping
    public ApiResponse<?> createMenu(@RequestBody CreateAdminMenuRequest request) {
        adminMenuService.create(request.toCommand());
        return ApiResponse.success();
    }

    @PostMapping("/option-group")
    public ApiResponse<?> createOptionGroup(@RequestBody CreateAdminOptionGroupRequest request) {
        adminMenuService.createOptionGroup(request.name(), request.isExclusive(), request.isRequired());
        return ApiResponse.success();
    }

    @PostMapping("/option")
    public ApiResponse<?> createOption(@RequestBody CreateAdminOptionRequest request) {
        adminMenuService.createOption(request.optionGroupId(), request.name(), request.extraPrice());
        return ApiResponse.success();
    }

    @PostMapping("/menu-option-group")
    public ApiResponse<?> createMenuOptionGroup(@RequestBody CreateAdminMenuOptionGroup request) {
        adminMenuService.createMenuOptionGroup(request.menuId(), request.optionGroupId());
        return ApiResponse.success();
    }

    @PutMapping("/{menuId}/option-groups")
    public ApiResponse<?> updateMenuOptionGroups(
            @PathVariable("menuId") Long menuId,
            @RequestBody UpdateAdminMenuOptionGroupRequest request
    ) {
        adminMenuService.updateMenuOptionGroups(menuId, request.optionGroupIds());
        return ApiResponse.success();
    }

    @PutMapping("/menu-option-group")
    public ApiResponse<?> updateMenuOptionGroup(@RequestBody UpdateAdminMenuOptionGroupRequest request) {
        adminMenuService.updateMenuOptionGroups(request.menuId(), request.optionGroupIds());
        return ApiResponse.success();
    }

}
