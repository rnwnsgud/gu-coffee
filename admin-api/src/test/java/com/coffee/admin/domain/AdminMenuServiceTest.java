package com.coffee.admin.domain;

import com.coffee.gu.*;
import com.coffee.gu.admin.domain.AdminEntityStatus;
import com.coffee.gu.admin.domain.AdminMenuService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.CacheEvict;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminMenuServiceTest {

    @Mock
    private AdminMenuRepository adminMenuRepository;

    @Mock
    private AdminOptionGroupRepository adminOptionGroupRepository;

    @Mock
    private AdminOptionRepository adminOptionRepository;

    @Mock
    private AdminMenuOptionGroupRepository adminMenuOptionGroupRepository;

    @InjectMocks
    private AdminMenuService adminMenuService;

    private final Long menuId = 1L;

    @Test
    @DisplayName("updateMenuOptionGroups 메서드에는 @CacheEvict(cacheNames='menuDetail', key='#menuId')가 선언되어 있어야 한다")
    void verifyCacheEvictAnnotationOnUpdateMenuOptionGroups() throws NoSuchMethodException {
        Method method = AdminMenuService.class.getMethod("updateMenuOptionGroups", Long.class, List.class);
        CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);

        assertThat(cacheEvict).isNotNull();
        assertThat(cacheEvict.cacheNames()).contains("menuDetail");
        assertThat(cacheEvict.key()).isEqualTo("#menuId");
    }

    @Test
    @DisplayName("메뉴 옵션 그룹 매핑 수정 시 기존 활성 매핑은 소프트 삭제되고 새 매핑들이 저장된다")
    void updateMenuOptionGroups_replacesExistingMappings() {
        // given
        AdminMenuOptionGroupEntity oldMapping1 = new AdminMenuOptionGroupEntity(menuId, 10L);
        AdminMenuOptionGroupEntity oldMapping2 = new AdminMenuOptionGroupEntity(menuId, 20L);
        given(adminMenuOptionGroupRepository.findByMenuIdAndEntityStatus(eq(menuId), eq(AdminEntityStatus.ACTIVE)))
                .willReturn(List.of(oldMapping1, oldMapping2));

        List<Long> newOptionGroupIds = List.of(20L, 30L);

        // when
        adminMenuService.updateMenuOptionGroups(menuId, newOptionGroupIds);

        // then
        assertThat(oldMapping1.isDeleted()).isTrue();
        assertThat(oldMapping2.isDeleted()).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AdminMenuOptionGroupEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(adminMenuOptionGroupRepository).saveAll(captor.capture());

        List<AdminMenuOptionGroupEntity> savedEntities = captor.getValue();
        assertThat(savedEntities).hasSize(2);
        assertThat(savedEntities).extracting(AdminMenuOptionGroupEntity::getMenuId)
                .containsOnly(menuId);
        assertThat(savedEntities).extracting(AdminMenuOptionGroupEntity::getOptionGroupId)
                .containsExactlyInAnyOrder(20L, 30L);
    }

    @Test
    @DisplayName("옵션 그룹 목록이 비어있으면 기존 매핑만 삭제되고 새 매핑은 저장되지 않는다")
    void updateMenuOptionGroups_withEmptyList_onlyDeletesExistingMappings() {
        // given
        AdminMenuOptionGroupEntity oldMapping = new AdminMenuOptionGroupEntity(menuId, 10L);
        given(adminMenuOptionGroupRepository.findByMenuIdAndEntityStatus(eq(menuId), eq(AdminEntityStatus.ACTIVE)))
                .willReturn(List.of(oldMapping));

        // when
        adminMenuService.updateMenuOptionGroups(menuId, List.of());

        // then
        assertThat(oldMapping.isDeleted()).isTrue();
    }
}
