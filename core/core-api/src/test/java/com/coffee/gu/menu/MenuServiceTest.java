package com.coffee.gu.menu;

import com.coffee.gu.OffsetLimit;
import com.coffee.gu.Page;
import com.coffee.gu.enums.MenuType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @InjectMocks
    private MenuService menuService;

    @Mock
    private MenuFinder menuFinder;

    @Mock
    private OptionFinder optionFinder;

    private Menu createMenu(Long id, String name) {
        return new Menu(
                id,
                name,
                MenuType.DRINK,
                new Price(BigDecimal.valueOf(1000), BigDecimal.valueOf(2000)),
                "http://image.com",
                "Description",
                new MenuDetail(
                        new Nutrition(355.0, 150.0, 10.0, 5.0, 2.0, 1.0, 0.0, 0.0, 0.5),
                        "Milk",
                        "Soy"
                )
        );
    }

    @Nested
    @DisplayName("findMenus 메서드는")
    class Describe_findMenus {

        @Test
        @DisplayName("카테고리 ID와 오프셋 기반 페이징 정보로 메뉴 목록을 조회한다")
        void it_returns_paged_menus_by_offset() {
            // given
            Long categoryId = 1L;
            OffsetLimit offsetLimit = new OffsetLimit(0, 10);
            List<Menu> menus = List.of(createMenu(1L, "Americano"));
            Page<Menu> page = new Page<>(menus, false, null, null);
            
            given(menuFinder.findByCategory(categoryId, offsetLimit)).willReturn(page);

            // when
            Page<Menu> result = menuService.findMenus(categoryId, offsetLimit);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).getName()).isEqualTo("Americano");
        }
    }

    @Nested
    @DisplayName("findMenuDetail 메서드는")
    class Describe_findMenuDetail {

        @Test
        @DisplayName("메뉴 ID로 메뉴 상세 정보, 옵션 그룹, 옵션을 포함한 응답을 반환한다")
        void it_returns_menu_detail_response() {
            // given
            Long menuId = 1L;
            Menu menu = createMenu(menuId, "Americano");
            OptionGroup optionGroup = new OptionGroup(10L, "Size", true, true);
            Option option = new Option(100L, 10L, "Tall", BigDecimal.ZERO);

            given(menuFinder.getById(menuId)).willReturn(menu);
            given(optionFinder.findByMenuId(menuId)).willReturn(List.of(optionGroup));
            given(optionFinder.findByOptionGroups(any())).willReturn(List.of(option));

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
}
