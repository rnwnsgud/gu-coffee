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
import org.mockito.kotlin.whenever
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
    @DisplayName("[Fail-Open] Redis 장애로 캐시 get 시 예외가 발생하더라도 500 에러를 내지 않고 DB 직접 조회로 자동 우회하여 정상 반환한다")
    fun testFailOpenOnRedisFailure() {
        // given: 원본 캐시를 모킹하여 Redis 장애 상황(RuntimeException) 시뮬레이션
        val mockCache = org.mockito.kotlin.mock<org.springframework.cache.Cache>()
        whenever(mockCache.name).thenReturn(CacheConfig.MENU_DETAIL_CACHE)
        whenever(mockCache.get(org.mockito.kotlin.any(), org.mockito.kotlin.any<java.util.concurrent.Callable<Any>>()))
            .thenThrow(org.springframework.data.redis.RedisConnectionFailureException("Redis connection refused"))

        // FailOpenCache 데코레이터 객체 생성
        val failOpenCacheClass = Class.forName("com.coffee.gu.config.CacheConfig\$FailOpenCache")
        val constructor = failOpenCacheClass.getDeclaredConstructor(org.springframework.cache.Cache::class.java)
        constructor.isAccessible = true
        val failOpenCache = constructor.newInstance(mockCache) as org.springframework.cache.Cache

        val dbCallable = java.util.concurrent.Callable {
            menuFinder.getById(menuId)
        }

        // when: Redis 장애 상태에서 캐시 조회 시도
        val result = failOpenCache.get(menuId, dbCallable)

        // then: 에러가 전파되지 않고 DB 조회를 호출해 정상 반환됨을 검증 (Fail-Open)
        assertThat(result).isNotNull
        assertThat(result?.name).isEqualTo("Latte")
        verify(menuFinder, times(1)).getById(menuId)
    }
}
