package com.coffee.gu.config

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import javax.sql.DataSource

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = ["com.coffee.gu"])
@Import(CoreDataSourceConfig::class, FlywayConfig::class, LocalDataInitializer::class)
class SeedDataTestApplication

@SpringBootTest(classes = [SeedDataTestApplication::class])
@TestPropertySource(
    properties = [
        "app.init-seed-data.enabled=true",
        "spring.flyway.enabled=true",
        "storage.datasource.core.driver-class-name=org.h2.Driver",
        "storage.datasource.core.jdbc-url=jdbc:h2:mem:seed_data_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "storage.datasource.core.username=sa",
        "storage.datasource.core.pool-name=seed-test-pool",
        "gu-coffee.storage.core.security.key=11111111111111111111111111111111",
        "gu-coffee.storage.core.security.iv=1111111111111111"
    ]
)
class LocalDataInitializerTest {

    @Autowired
    private lateinit var coreDataSource: DataSource

    @Autowired
    private lateinit var localDataInitializer: LocalDataInitializer

    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setUp() {
        jdbcTemplate = JdbcTemplate(coreDataSource)
    }

    @Test
    @DisplayName("최초 실행 시 seed-data.sql이 실행되어 카테고리, 메뉴, 옵션, 매장, 쿠폰 데이터가 적재된다")
    fun testSeedDataPopulatedOnFirstRun() {
        val categoryCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM category", Long::class.java)
        val menuCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM menu", Long::class.java)
        val optionGroupCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM option_group", Long::class.java)
        val optionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `option`", Long::class.java)
        val storeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM store", Long::class.java)
        val couponCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM coupon", Long::class.java)
        val issuedCouponCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM issued_coupon WHERE principal_key = 'U1'", Long::class.java)
        val stampCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stamp WHERE principal_key = 'U1'", Long::class.java)

        assertThat(categoryCount).isEqualTo(4L)
        assertThat(menuCount).isEqualTo(5L)
        assertThat(optionGroupCount).isEqualTo(4L)
        assertThat(optionCount).isEqualTo(9L)
        assertThat(storeCount).isEqualTo(2L)
        assertThat(couponCount).isEqualTo(3L)
        assertThat(issuedCouponCount).isEqualTo(2L)
        assertThat(stampCount).isEqualTo(3L)
    }

    @Test
    @DisplayName("데이터가 이미 존재하는 상태에서 다시 실행해도 중복 삽입되지 않고 스킵된다 (멱등성 보장)")
    fun testSubsequentRunSkipsWhenDataExists() {
        val beforeMenuCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM menu", Long::class.java) ?: 0L
        assertThat(beforeMenuCount).isGreaterThan(0L)

        // 두 번째 수동 실행 시도
        localDataInitializer.run(org.springframework.boot.DefaultApplicationArguments())

        val afterMenuCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM menu", Long::class.java) ?: 0L
        assertThat(afterMenuCount).isEqualTo(beforeMenuCount)
    }
}
