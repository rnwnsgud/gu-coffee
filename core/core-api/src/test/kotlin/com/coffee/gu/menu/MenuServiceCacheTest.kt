package com.coffee.gu.menu

import com.coffee.gu.TestApplication
import com.coffee.gu.config.CacheConfig
import com.coffee.gu.enums.MenuType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal

@SpringBootTest(classes = [TestApplication::class])
@ActiveProfiles("local")
class MenuServiceCacheTest {

    @Autowired
    private lateinit var menuService: MenuService

    @Autowired
    private lateinit var cacheManager: CacheManager

    @MockitoBean
    private lateinit var menuFinder: MenuFinder

    @MockitoBean
    private lateinit var optionFinder: OptionFinder

    private val menuId = 999L

    @BeforeEach
    fun setUp() {
        cacheManager.getCache(CacheConfig.MENU_DETAIL_CACHE)?.clear()

        val menu = Menu(
            id = menuId,
            name = "Latte",
            type = MenuType.DRINK,
            price = Price(BigDecimal.valueOf(1500), BigDecimal.valueOf(3000)),
            imageUrl = null,
            description = null,
            detail = null
        )
        given(menuFinder.getById(menuId)).willReturn(menu)
        given(optionFinder.findByMenuId(menuId)).willReturn(emptyList())
        given(optionFinder.findByOptionGroups(emptyList())).willReturn(emptyList())
    }

    @Test
    @DisplayName("최초 조회 시 DB 쿼리를 수행하고, 두 번째 조회 시 캐시에서 즉시 반환하여 DB 조회를 하지 않는다")
    fun testCacheHitAvoidsDbQuery() {
        // 1회차: Cache Miss -> DB 조회 실행
        val firstResult = menuService.getMenu(menuId)
        assertThat(firstResult.menu.name).isEqualTo("Latte")
        verify(menuFinder, times(1)).getById(menuId)
        verify(optionFinder, times(1)).findByMenuId(menuId)

        clearInvocations(menuFinder, optionFinder)

        // 2회차: Cache Hit -> DB 조회 생략
        val secondResult = menuService.getMenu(menuId)
        assertThat(secondResult.menu.name).isEqualTo("Latte")
        verify(menuFinder, times(0)).getById(menuId)
        verify(optionFinder, times(0)).findByMenuId(menuId)
    }

    @Test
    @DisplayName("evictMenuDetail 호출 시 캐시가 무효화되어 다음 조회 시 다시 DB 조회를 수행한다")
    fun testCacheEvictForcesDbQuery() {
        // 1회차: 캐시 적재
        menuService.getMenu(menuId)
        verify(menuFinder, times(1)).getById(menuId)

        clearInvocations(menuFinder, optionFinder)

        // 캐시 무효화 (Evict)
        menuService.evictMenuDetail(menuId)

        // 3회차: 캐시가 비었으므로 다시 DB 조회 실행
        val afterEvictResult = menuService.getMenu(menuId)
        assertThat(afterEvictResult.menu.name).isEqualTo("Latte")
        verify(menuFinder, times(1)).getById(menuId)
    }
}
