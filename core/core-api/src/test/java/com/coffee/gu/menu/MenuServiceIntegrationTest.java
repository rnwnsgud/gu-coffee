package com.coffee.gu.menu;

import com.coffee.gu.enums.MenuType;
import com.coffee.gu.OffsetLimit;
import com.coffee.gu.Page;
import com.coffee.gu.TestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestApplication.class)
@Transactional
@ActiveProfiles("local")
class MenuServiceIntegrationTest {

    @Autowired
    private MenuService menuService;

    @Autowired
    private MenuJpaRepository menuRepository;

    @Autowired
    private MenuCategoryJpaRepository menuCategoryRepository;

    @Autowired
    private OptionGroupJpaRepository optionGroupRepository;

    @Autowired
    private OptionJpaRepository optionRepository;

    @Autowired
    private MenuOptionGroupJpaRepository menuOptionGroupRepository;

    private Long categoryId = 1L;
    private Long menuId;

    @BeforeEach
    void setUp() throws Exception {
        MenuEntity menuEntity = new MenuEntity();
        ReflectionTestUtils.setField(menuEntity, "name", "Americano");
        ReflectionTestUtils.setField(menuEntity, "type", MenuType.DRINK);
        ReflectionTestUtils.setField(menuEntity, "costPrice", BigDecimal.valueOf(1000));
        ReflectionTestUtils.setField(menuEntity, "salesPrice", BigDecimal.valueOf(2000));
        ReflectionTestUtils.setField(menuEntity, "description", "Description");
        ReflectionTestUtils.setField(menuEntity, "createdAt", LocalDateTime.now());
        menuEntity.active();

        menuRepository.saveAndFlush(menuEntity);
        menuId = menuEntity.getId();

        // MenuCategory 연결
        MenuCategoryEntity menuCategoryEntity = new MenuCategoryEntity(menuId, categoryId);
        menuCategoryRepository.saveAndFlush(menuCategoryEntity);

        // OptionGroup 생성
        OptionGroupEntity optionGroupEntity = new OptionGroupEntity("Size", true, true);
        optionGroupRepository.saveAndFlush(optionGroupEntity);
        Long optionGroupId = optionGroupEntity.getId();

        // MenuOptionGroup 연결
        MenuOptionGroupEntity menuOptionGroupEntity = new MenuOptionGroupEntity(menuId, optionGroupId);
        menuOptionGroupRepository.saveAndFlush(menuOptionGroupEntity);

        // Option 생성
        OptionEntity optionEntity = new OptionEntity(optionGroupId, "Tall", BigDecimal.ZERO);
        optionRepository.saveAndFlush(optionEntity);
    }

    @Test
    @DisplayName("카테고리별 메뉴 목록을 조회할 수 있다")
    void findMenus() {
        // given
        OffsetLimit offsetLimit = new OffsetLimit(0, 10);

        // when
        Page<Menu> result = menuService.findMenus(categoryId, offsetLimit);

        // then
        assertThat(result.content()).isNotEmpty();
        assertThat(result.content().get(0).getName()).isEqualTo("Americano");
    }

    @Test
    @DisplayName("메뉴 상세 정보를 조회할 수 있다")
    void getMenuDeatil() {
        // when
        MenuDetailResult response = menuService.getMenu(menuId);

        // then
        assertThat(response.menu().getId()).isEqualTo(menuId);
        assertThat(response.menu().getName()).isEqualTo("Americano");
        assertThat(response.optionGroups()).hasSize(1);
        assertThat(response.optionGroups().get(0).getName()).isEqualTo("Size");
        assertThat(response.options()).hasSize(1);
        assertThat(response.options().get(0).getName()).isEqualTo("Tall");
    }
}
