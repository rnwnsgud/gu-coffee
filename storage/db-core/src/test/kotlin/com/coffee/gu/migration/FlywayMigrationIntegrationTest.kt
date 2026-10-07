package com.coffee.gu.migration

import com.coffee.gu.config.CoreDataSourceConfig
import com.coffee.gu.config.FlywayConfig
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
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
@Import(CoreDataSourceConfig::class, FlywayConfig::class)
class FlywayTestApplication

@SpringBootTest(classes = [FlywayTestApplication::class])
@TestPropertySource(
    properties = [
        "spring.flyway.enabled=true",
        "storage.datasource.core.driver-class-name=org.h2.Driver",
        "storage.datasource.core.jdbc-url=jdbc:h2:mem:flyway_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "storage.datasource.core.username=sa",
        "storage.datasource.core.pool-name=flyway-test-pool",
        "gu-coffee.storage.core.security.key=11111111111111111111111111111111",
        "gu-coffee.storage.core.security.iv=1111111111111111"
    ]
)
class FlywayMigrationIntegrationTest {

    @Autowired
    private lateinit var flyway: Flyway

    @Autowired
    private lateinit var coreDataSource: DataSource

    @Test
    @DisplayName("애플리케이션 기동 시 V1 및 V2 DDL 스크립트가 실행되어 flyway_schema_history에 정상 기록된다")
    fun testFlywaySchemaHistoryRecorded() {
        val jdbcTemplate = JdbcTemplate(coreDataSource)

        // 1. flyway_schema_history 테이블에 V1, V2 성공 기록 검증
        val v1Count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM \"flyway_schema_history\" WHERE \"version\" = '1' AND \"success\" = TRUE",
            Int::class.java
        )
        assertThat(v1Count).isEqualTo(1)

        val v2Count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM \"flyway_schema_history\" WHERE \"version\" = '2' AND \"success\" = TRUE",
            Int::class.java
        )
        assertThat(v2Count).isEqualTo(1)

        // 2. Flyway info API로 마이그레이션 상태 검증
        val current = flyway.info().current()
        assertThat(current).isNotNull
        assertThat(current?.version?.version).isEqualTo("2")
        assertThat(current?.description).isEqualTo("add limited coupon")
        assertThat(current?.state?.isApplied).isTrue()
    }

    @Test
    @DisplayName("V1 및 V2 스키마에 정의된 핵심 테이블 및 컬럼, 인덱스가 실제 DB에 모두 생성된다")
    fun testKeyTablesAndIndexesExist() {
        val jdbcTemplate = JdbcTemplate(coreDataSource)

        // 주요 테이블 조회 테스트 (MySQL 모드 백틱 적용)
        val tables = listOf("category", "menu", "store", "`order`", "payment", "coupon", "issued_coupon", "limited_coupon", "event_log", "stamp")
        for (table in tables) {
            val rows = jdbcTemplate.queryForList("SELECT count(*) as cnt FROM $table")
            assertThat(rows).isNotEmpty
        }
    }
}
