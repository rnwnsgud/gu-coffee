package com.coffee.gu.migration

import com.coffee.gu.config.CoreDataSourceConfig
import com.coffee.gu.config.CoreJpaConfig
import com.coffee.gu.config.FlywayConfig
import com.coffee.gu.config.QuerydslConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import javax.sql.DataSource

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = ["com.coffee.gu"])
@EntityScan(basePackages = ["com.coffee.gu"])
@EnableJpaRepositories(basePackages = ["com.coffee.gu"])
@Import(CoreDataSourceConfig::class, CoreJpaConfig::class, QuerydslConfig::class, FlywayConfig::class)
class JpaSchemaValidateApplication

@SpringBootTest(classes = [JpaSchemaValidateApplication::class])
@TestPropertySource(
    properties = [
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.show_sql=false",
        "storage.datasource.core.driver-class-name=org.h2.Driver",
        "storage.datasource.core.jdbc-url=jdbc:h2:mem:validate_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "storage.datasource.core.username=sa",
        "storage.datasource.core.pool-name=validate-test-pool",
        "gu-coffee.storage.core.security.key=11111111111111111111111111111111",
        "gu-coffee.storage.core.security.iv=1111111111111111"
    ]
)
class FlywayJpaSchemaValidationTest {

    @Autowired
    private lateinit var coreDataSource: DataSource

    @Test
    @DisplayName("ddl-auto: validate 환경에서 Flyway가 생성한 스키마와 21개 JPA 엔티티가 100% 일치하여 성공한다")
    fun testFlywaySchemaPassesJpaValidation() {
        val jdbcTemplate = JdbcTemplate(coreDataSource)

        // flyway_schema_history에 정상 기록 확인
        val count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM \"flyway_schema_history\" WHERE \"success\" = TRUE",
            Int::class.java
        )
        assertThat(count).isGreaterThanOrEqualTo(1)
    }
}
