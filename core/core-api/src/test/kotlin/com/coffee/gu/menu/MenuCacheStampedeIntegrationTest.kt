package com.coffee.gu.menu

import com.coffee.gu.config.CacheConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class MenuCacheStampedeIntegrationTest {

    @Autowired
    private lateinit var menuService: MenuService

    @MockitoSpyBean
    private lateinit var menuFinder: MenuFinder

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private var testMenuId: Long = 0L

    @BeforeEach
    fun setUp() {
        // 캐시 초기화
        cacheManager.getCache(CacheConfig.MENU_DETAIL_CACHE)?.clear()

        // DB 초기화 및 메뉴 1건 생성
        jdbcTemplate.update("DELETE FROM menu")
        jdbcTemplate.update(
            """
            INSERT INTO menu (name, type, cost_price, sales_price, entity_status, created_at, updated_at)
            VALUES ('시그니처 아메리카노', 'DRINK', 1000, 4500, 'ACTIVE', NOW(), NOW())
            """.trimIndent()
        )
        testMenuId = jdbcTemplate.queryForObject("SELECT id FROM menu LIMIT 1", Long::class.java)!!
    }

    @Test
    @DisplayName("[캐시 스탬피드 회귀 방지] sync=true 설정 시 50개 스레드가 동시에 캐시 미스 상태에서 메뉴를 조회해도 실제 DB 조인/조회는 단 1회만 발생하고 49개는 캐시를 공유한다")
    fun testCacheStampedeProtectionWithSyncTrue() {
        val totalThreads = 50
        val executor = Executors.newFixedThreadPool(16)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(totalThreads)

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        for (i in 0 until totalThreads) {
            executor.submit {
                try {
                    startLatch.await() // 50개 스레드 동시 출발
                    val result = menuService.getMenu(testMenuId)
                    if (result.menu.id == testMenuId) {
                        successCount.incrementAndGet()
                    }
                } catch (e: Exception) {
                    failureCount.incrementAndGet()
                    e.printStackTrace()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        println("=== [캐시 스탬피드 50개 동시 요청 실측 결과] ===")
        println("성공 수: ${successCount.get()}")
        println("실패 수: ${failureCount.get()}")

        // 1. 모든 50개 요청은 정상 응답을 받아야 함
        assertThat(successCount.get()).isEqualTo(totalThreads)
        assertThat(failureCount.get()).isEqualTo(0)

        // 2. sync = true 락 동기화 검증: 50개 스레드가 동시 요청했음에도 menuFinder.getById는 정확히 1번만 실행되어야 함
        verify(menuFinder, times(1)).getById(testMenuId)

        // 3. 캐시에 결과가 정상 저장되어 있는지 확인
        val cachedValue = cacheManager.getCache(CacheConfig.MENU_DETAIL_CACHE)?.get(testMenuId)?.get()
        assertThat(cachedValue).isNotNull
    }
}
