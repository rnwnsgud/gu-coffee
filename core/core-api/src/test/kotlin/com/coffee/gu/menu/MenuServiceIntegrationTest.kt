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
    @DisplayName("커서 기반 페이지네이션: 첫 페이지 조회 시 다음 페이지 여부(hasNext=true)와 다음 커서(nextCursor, nextLastId)를 정상 반환한다")
    fun findMenus_cursor_firstPage() {
        // given: 3개의 추가 메뉴 등록 (총 4개 메뉴)
        for (i in 1..3) {
            val menu = MenuEntity(
                name = "Menu_$i",
                type = MenuType.DRINK,
                costPrice = BigDecimal.valueOf(1000),
                salesPrice = BigDecimal.valueOf(2000),
                description = "Desc $i"
            )
            menu.active()
            menuRepository.saveAndFlush(menu)
            menuCategoryRepository.saveAndFlush(MenuCategoryEntity(menuId = menu.id, categoryId = categoryId))
        }

        // when: pageSize = 2로 첫 페이지 조회 (cursor = null, lastId = null)
        val firstPage = menuService.findMenus(categoryId, pageSize = 2, cursor = null, lastId = null)

        // then
        assertThat(firstPage.content).hasSize(2)
        assertThat(firstPage.hasNext).isTrue()
        assertThat(firstPage.nextCursor).isNotNull
        assertThat(firstPage.nextLastId).isEqualTo(firstPage.content.last().id)
    }

    @Test
    @DisplayName("커서 기반 페이지네이션: 이전 페이지에서 전달받은 커서로 다음 페이지 조회 시 중복 없이 연속된 데이터가 조회된다")
    fun findMenus_cursor_continuousPaging() {
        // given: 4개의 추가 메뉴 등록 (총 5개 메뉴)
        for (i in 1..4) {
            val menu = MenuEntity(
                name = "Menu_$i",
                type = MenuType.DRINK,
                costPrice = BigDecimal.valueOf(1000),
                salesPrice = BigDecimal.valueOf(2000),
                description = "Desc $i"
            )
            menu.active()
            menuRepository.saveAndFlush(menu)
            menuCategoryRepository.saveAndFlush(MenuCategoryEntity(menuId = menu.id, categoryId = categoryId))
        }

        // 1페이지 조회
        val page1 = menuService.findMenus(categoryId, pageSize = 2, cursor = null, lastId = null)
        assertThat(page1.content).hasSize(2)
        assertThat(page1.hasNext).isTrue()

        // 2페이지 조회 (page1의 커서 사용)
        val page2 = menuService.findMenus(categoryId, pageSize = 2, cursor = page1.nextCursor, lastId = page1.nextLastId)
        assertThat(page2.content).hasSize(2)
        assertThat(page2.hasNext).isTrue()

        // 1페이지와 2페이지 간 데이터 중복이 전혀 없음을 검증
        val page1Ids = page1.content.map { it.id }.toSet()
        val page2Ids = page2.content.map { it.id }.toSet()
        assertThat(page1Ids.intersect(page2Ids)).isEmpty()

        // 3페이지 조회 (마지막 페이지)
        val page3 = menuService.findMenus(categoryId, pageSize = 2, cursor = page2.nextCursor, lastId = page2.nextLastId)
        assertThat(page3.content).hasSize(1)
        assertThat(page3.hasNext).isFalse()
        assertThat(page3.nextCursor).isNull()
        assertThat(page3.nextLastId).isNull()
    }

    @Test
    @DisplayName("커서 기반 페이지네이션: 동일한 createdAt을 가진 복수 메뉴가 있을 때 id DESC tie-breaker로 중복 및 누락 없이 전건 조회된다")
    fun findMenus_cursor_tieBreakerSameCreatedAt() {
        val fixedTime = LocalDateTime.of(2026, 10, 1, 12, 0, 0)

        // given: 동일한 createdAt을 가진 3개의 메뉴 저장
        val sameTimeMenus = (1..3).map { i ->
            val menu = MenuEntity(
                name = "SameTimeMenu_$i",
                type = MenuType.DRINK,
                costPrice = BigDecimal.valueOf(1000),
                salesPrice = BigDecimal.valueOf(2000),
            )
            menu.active()
            ReflectionTestUtils.setField(menu, "createdAt", fixedTime)
            menuRepository.saveAndFlush(menu)
            menuCategoryRepository.saveAndFlush(MenuCategoryEntity(menuId = menu.id, categoryId = categoryId))
            menu
        }.sortedByDescending { it.id }

        // when: pageSize = 1로 동일 시각 메뉴들을 순차 커서 페이징 조회
        val fetchedIds = mutableListOf<Long>()
        var currentCursor: LocalDateTime? = null
        var currentLastId: Long? = null
        var hasNext = true

        while (hasNext) {
            val page = menuService.findMenus(categoryId, pageSize = 1, cursor = currentCursor, lastId = currentLastId)
            fetchedIds.addAll(page.content.map { it.id })
            currentCursor = page.nextCursor
            currentLastId = page.nextLastId
            hasNext = page.hasNext
        }

        // then: 동일한 시각을 가졌더라도 id DESC에 의해 동일 시각 메뉴 3개가 누락/중복 없이 모두 조회되었는지 검증
        assertThat(fetchedIds).containsAll(sameTimeMenus.map { it.id })
        val expectedSameTimeOrder = sameTimeMenus.map { it.id }
        val actualSameTimeOrder = fetchedIds.filter { expectedSameTimeOrder.contains(it) }
        assertThat(actualSameTimeOrder).isEqualTo(expectedSameTimeOrder)
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
