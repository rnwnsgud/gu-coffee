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
    @DisplayName("V1 적용 후 V2 스크립트를 추가하여 migrate()를 재실행하면 flyway_schema_history에 순차적으로 기록된다")
    fun testSequentialMigration() {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:seq_flyway_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
            username = "sa"
            password = ""
            driverClassName = "org.h2.Driver"
        }
        val dataSource = HikariDataSource(config)
        val jdbcTemplate = JdbcTemplate(dataSource)

        // 1. V1만 존재하는 위치(기본 classpath:db/migration)로 1차 마이그레이션 실행
        val flywayV1 = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()

        val v1Result = flywayV1.migrate()
        assertThat(v1Result.migrationsExecuted).isEqualTo(1)

        // V1 실행 직후 flyway_schema_history 검증 (installed_rank > 0 인 실제 마이그레이션 레코드)
        val v1Migrations = jdbcTemplate.queryForList("SELECT installed_rank, version, description, type, script, success FROM \"flyway_schema_history\" WHERE \"version\" IS NOT NULL ORDER BY installed_rank")
        assertThat(v1Migrations).hasSize(1)
        assertThat(v1Migrations[0]["version"]).isEqualTo("1")
        assertThat(v1Migrations[0]["description"]).isEqualTo("init schema")
        println("=== [1차 마이그레이션 직후 flyway_schema_history (V1 적용)] ===")
        v1Migrations.forEach { println(it) }

        // 2. V2 스크립트가 추가된 위치(classpath:db/migration_seq_test)를 추가하여 2차 마이그레이션 실행
        val flywayV2 = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration", "classpath:db/migration_seq_test")
            .load()

        val v2Result = flywayV2.migrate()
        assertThat(v2Result.migrationsExecuted).isEqualTo(1) // V1은 이미 실행되었으므로 V2 1개만 실행됨!

        // V2 실행 직후 flyway_schema_history 검증
        val v2Migrations = jdbcTemplate.queryForList("SELECT installed_rank, version, description, type, script, success FROM \"flyway_schema_history\" WHERE \"version\" IS NOT NULL ORDER BY installed_rank")
        assertThat(v2Migrations).hasSize(2)
        assertThat(v2Migrations[1]["version"]).isEqualTo("2")
        assertThat(v2Migrations[1]["description"]).isEqualTo("add store test memo")
        println("=== [2차 마이그레이션 직후 flyway_schema_history (V1 + V2 순차 누적)] ===")
        v2Migrations.forEach { println(it) }

        // V2에 의해 실제로 컬럼이 추가되었는지 검증 (예: store 테이블에 test_col 추가)
        val columnCheck = jdbcTemplate.queryForList("SELECT test_memo FROM store")
        assertThat(columnCheck).isNotNull

        // Flyway API 상태 객체 검증
        val current = flywayV2.info().current()
        assertThat(current?.version?.version).isEqualTo("2")
        assertThat(current?.description).isEqualTo("add store test memo")
        assertThat(current?.state?.isApplied).isTrue()
    }
}
