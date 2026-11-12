package com.coffee.gu.migration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate

class FlywaySequentialMigrationTest {

    @Test
    @DisplayName("V1~V4 적용 후 V5 스크립트를 추가하여 migrate()를 재실행하면 flyway_schema_history에 순차적으로 기록된다")
    fun testSequentialMigration() {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:seq_flyway_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
            username = "sa"
            password = ""
            driverClassName = "org.h2.Driver"
        }
        val dataSource = HikariDataSource(config)
        val jdbcTemplate = JdbcTemplate(dataSource)

        // 1. 기본 프로덕션 마이그레이션(classpath:db/migration -> V1, V2, V3, V4)으로 1차 실행
        val flywayV1ToV4 = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()

        val v1ToV4Result = flywayV1ToV4.migrate()
        assertThat(v1ToV4Result.migrationsExecuted).isEqualTo(4)

        // V1~V4 실행 직후 flyway_schema_history 검증
        val initialMigrations = jdbcTemplate.queryForList("SELECT installed_rank, version, description, type, script, success FROM \"flyway_schema_history\" WHERE \"version\" IS NOT NULL ORDER BY installed_rank")
        assertThat(initialMigrations).hasSize(4)
        assertThat(initialMigrations[0]["version"]).isEqualTo("1")
        assertThat(initialMigrations[0]["description"]).isEqualTo("init schema")
        assertThat(initialMigrations[1]["version"]).isEqualTo("2")
        assertThat(initialMigrations[1]["description"]).isEqualTo("add limited coupon")
        assertThat(initialMigrations[2]["version"]).isEqualTo("3")
        assertThat(initialMigrations[2]["description"]).isEqualTo("add limited menu")
        assertThat(initialMigrations[3]["version"]).isEqualTo("4")
        assertThat(initialMigrations[3]["description"]).isEqualTo("add ingredient and recipe")
        println("=== [1차 마이그레이션 직후 flyway_schema_history (V1~V4 적용)] ===")
        initialMigrations.forEach { println(it) }

        // 2. V5 스크립트가 추가된 위치(classpath:db/migration_seq_test)를 추가하여 2차 순차 마이그레이션 실행
        val flywayV5 = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration", "classpath:db/migration_seq_test")
            .load()

        val v5Result = flywayV5.migrate()
        assertThat(v5Result.migrationsExecuted).isEqualTo(1) // V1~V4는 이미 실행되었으므로 V5 1개만 실행됨!

        // V5 실행 직후 flyway_schema_history 검증
        val subsequentMigrations = jdbcTemplate.queryForList("SELECT installed_rank, version, description, type, script, success FROM \"flyway_schema_history\" WHERE \"version\" IS NOT NULL ORDER BY installed_rank")
        assertThat(subsequentMigrations).hasSize(5)
        assertThat(subsequentMigrations[4]["version"]).isEqualTo("5")
        assertThat(subsequentMigrations[4]["description"]).isEqualTo("add store test memo")
        println("=== [2차 마이그레이션 직후 flyway_schema_history (V1~V5 순차 누적)] ===")
        subsequentMigrations.forEach { println(it) }

        // V5에 의해 실제로 컬럼이 추가되었는지 검증 (예: store 테이블에 test_memo 추가)
        val columnCheck = jdbcTemplate.queryForList("SELECT test_memo FROM store")
        assertThat(columnCheck).isNotNull

        // Flyway API 상태 객체 검증
        val current = flywayV5.info().current()
        assertThat(current?.version?.version).isEqualTo("5")
        assertThat(current?.description).isEqualTo("add store test memo")
        assertThat(current?.state?.isApplied).isTrue()
    }
}
