package com.coffee.gu.menu

import com.coffee.gu.OffsetLimit
import com.coffee.gu.Page
import com.coffee.gu.TestApplication
import com.coffee.gu.enums.MenuType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest(classes = [TestApplication::class])
@Transactional
@ActiveProfiles("local")
class MenuServiceIntegrationTest {

    @Autowired
    private lateinit var menuService: MenuService

    @Autowired
    private lateinit var menuRepository: MenuJpaRepository

    @Autowired
    private lateinit var menuCategoryRepository: MenuCategoryJpaRepository

    @Autowired
    private lateinit var optionGroupRepository: OptionGroupJpaRepository

    @Autowired
    private lateinit var optionRepository: OptionJpaRepository

    @Autowired
    private lateinit var menuOptionGroupRepository: MenuOptionGroupJpaRepository

    private val categoryId = 1L
    private var menuId: Long = 0L

    @BeforeEach
    fun setUp() {
        val menuEntity = MenuEntity(
            name = "Americano",
            type = MenuType.DRINK,
            costPrice = BigDecimal.valueOf(1000),
            salesPrice = BigDecimal.valueOf(2000),
            description = "Description"
        )
        menuEntity.active()

        menuRepository.saveAndFlush(menuEntity)
        menuId = menuEntity.id

        // MenuCategory 연결
        val menuCategoryEntity = MenuCategoryEntity(menuId = menuId, categoryId = categoryId)
        menuCategoryRepository.saveAndFlush(menuCategoryEntity)

        // OptionGroup 생성
        val optionGroupEntity = OptionGroupEntity(name = "Size", isExclusive = true, isRequired = true)
        optionGroupRepository.saveAndFlush(optionGroupEntity)
        val optionGroupId = optionGroupEntity.id

        // MenuOptionGroup 연결
        val menuOptionGroupEntity = MenuOptionGroupEntity(menuId = menuId, optionGroupId = optionGroupId)
        menuOptionGroupRepository.saveAndFlush(menuOptionGroupEntity)

        // Option 생성
        val optionEntity = OptionEntity(optionGroupId = optionGroupId, name = "Tall", extraPrice = BigDecimal.ZERO)
        optionRepository.saveAndFlush(optionEntity)
    }

    @Test
    @DisplayName("카테고리별 메뉴 목록을 조회할 수 있다")
    fun findMenus() {
        // given
        val offsetLimit = OffsetLimit(0, 10)

        // when
        val result = menuService.findMenus(categoryId, offsetLimit)

        // then
        assertThat(result.content).isNotEmpty
        assertThat(result.content[0].name).isEqualTo("Americano")
    }

    @Test
    @DisplayName("메뉴 상세 정보를 조회할 수 있다")
    fun getMenuDeatil() {
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
