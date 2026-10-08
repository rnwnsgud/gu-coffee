package com.coffee.admin.controller;

import com.coffee.gu.admin.controller.AdminMenuController;
import com.coffee.gu.admin.controller.request.UpdateAdminMenuOptionGroupRequest;
import com.coffee.gu.admin.domain.AdminMenuService;
import com.coffee.gu.admin.support.response.ApiResponse;
import com.coffee.gu.admin.support.response.StatusType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminMenuControllerTest {

    @Mock
    private AdminMenuService adminMenuService;

    @InjectMocks
    private AdminMenuController adminMenuController;

    @Test
    @DisplayName("PUT /{menuId}/option-groups 호출 시 adminMenuService.updateMenuOptionGroups가 호출된다")
    void updateMenuOptionGroups_callsService() {
        // given
        Long menuId = 1L;
        UpdateAdminMenuOptionGroupRequest request = new UpdateAdminMenuOptionGroupRequest(List.of(10L, 20L));

        // when
        ApiResponse<?> response = adminMenuController.updateMenuOptionGroups(menuId, request);

        // then
        assertThat(response.status()).isEqualTo(StatusType.SUCCESS);
        verify(adminMenuService).updateMenuOptionGroups(menuId, List.of(10L, 20L));
    }

    @Test
    @DisplayName("PUT /menu-option-group 호출 시 request의 menuId로 adminMenuService.updateMenuOptionGroups가 호출된다")
    void updateMenuOptionGroup_callsServiceWithBodyMenuId() {
        // given
        Long menuId = 2L;
        UpdateAdminMenuOptionGroupRequest request = new UpdateAdminMenuOptionGroupRequest(menuId, List.of(30L, 40L));

        // when
        ApiResponse<?> response = adminMenuController.updateMenuOptionGroup(request);

        // then
        assertThat(response.status()).isEqualTo(StatusType.SUCCESS);
        verify(adminMenuService).updateMenuOptionGroups(menuId, List.of(30L, 40L));
    }
}
