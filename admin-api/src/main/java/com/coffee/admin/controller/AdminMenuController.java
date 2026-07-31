package com.coffee.admin.controller;

import com.coffee.admin.controller.request.CreateAdminMenuOptionGroup;
import com.coffee.admin.controller.request.CreateAdminOptionGroupRequest;
import com.coffee.admin.controller.request.CreateAdminOptionRequest;
import com.coffee.admin.domain.AdminMenuService;
import com.coffee.admin.controller.request.CreateAdminMenuRequest;
import com.coffee.admin.support.response.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
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

}
