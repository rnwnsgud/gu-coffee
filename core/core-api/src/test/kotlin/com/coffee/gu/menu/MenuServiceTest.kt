package com.coffee.gu.menu

import com.coffee.gu.OffsetLimit
import com.coffee.gu.Page
import com.coffee.gu.enums.MenuType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class MenuServiceTest {

    @InjectMocks
    private lateinit var menuService: MenuService

    @Mock
    private lateinit var menuFinder: MenuFinder

    @Mock
    private lateinit var optionFinder: OptionFinder

    private fun createMenu(id: Long, name: String): Menu {
        return Menu(
            id,
            name,
            MenuType.DRINK,
            Price(BigDecimal.valueOf(1000), BigDecimal.valueOf(2000)),
            "http://image.com",
            "Description",
            MenuDetail(
                Nutrition(355.0, 150.0, 10.0, 5.0, 2.0, 1.0, 0.0, 0.0, 0.5),
                "Milk",
                "Soy"
            )
        )
    }

    @Nested
    @DisplayName("findMenus 메서드는")
    inner class Describe_findMenus {

        @Test
        @DisplayName("카테고리 ID와 오프셋 기반 페이징 정보로 메뉴 목록을 조회한다")
        fun it_returns_paged_menus_by_offset() {
            // given
            val categoryId = 1L
            val offsetLimit = OffsetLimit(0, 10)
            val menus = listOf(createMenu(1L, "Americano"))
            val page = Page(menus, false, null, null)

            given(menuFinder.findByCategory(categoryId, offsetLimit)).willReturn(page)

            // when
            val result = menuService.findMenus(categoryId, offsetLimit)

            // then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("Americano")
        }
    }

    @Nested
    @DisplayName("findMenuDetail 메서드는")
    inner class Describe_findMenuDetail {

        @Test
        @DisplayName("메뉴 ID로 메뉴 상세 정보, 옵션 그룹, 옵션을 포함한 응답을 반환한다")
        fun it_returns_menu_detail_response() {
            // given
            val menuId = 1L
            val menu = createMenu(menuId, "Americano")
            val optionGroup = OptionGroup(10L, "Size", true, true)
            val option = Option(100L, 10L, "Tall", BigDecimal.ZERO)

            given(menuFinder.getById(menuId)).willReturn(menu)
            given(optionFinder.findByMenuId(menuId)).willReturn(listOf(optionGroup))
            given(optionFinder.findByOptionGroups(any())).willReturn(listOf(option))

            // when
            val response = menuService.getMenu(menuId)

            // then
            assertThat(response.menu.id).isEqualTo(menuId)
            assertThat(response.menu.name).isEqualTo("Americano")
            assertThat(response.optionGroups).hasSize(1)
            assertThat(response.optionGroups[0].name).isEqualTo("Size")
            assertThat(response.options).hasSize(1)
            assertThat(response.options[0].name).isEqualTo("Tall")
        }
    }
}
