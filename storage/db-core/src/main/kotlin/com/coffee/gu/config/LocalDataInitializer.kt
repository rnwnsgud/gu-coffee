package com.coffee.gu.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@ConditionalOnProperty(name = ["app.init-seed-data.enabled"], havingValue = "true")
class LocalDataInitializer(
    @param:Qualifier("coreDataSource") private val dataSource: DataSource,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val jdbcTemplate = JdbcTemplate(dataSource)

        val menuCount = try {
            jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM menu") ?: 0L
        } catch (e: Exception) {
            log.warn("☕ [LocalDataInitializer] 테이블 조회 실패 (테이블 미생성 등): {}", e.message)
            return
        }

        if (menuCount > 0L) {
            log.info("☕ [LocalDataInitializer] 이미 초기 데이터(메뉴 {}건)가 존재하므로 시드 데이터 투입을 건너뜁니다.", menuCount)
            return
        }

        log.info("☕ [LocalDataInitializer] 빈 데이터베이스가 감지되었습니다. 로컬 개발/테스트용 초기 시드 데이터를 적재합니다...")

        try {
            val populator = ResourceDatabasePopulator(ClassPathResource("seed-data.sql"))
            populator.execute(dataSource)
            log.info("☕ [LocalDataInitializer] 초기 시드 데이터 적재 완료! (카테고리 4건, 메뉴 5건, 옵션 9건, 매장 2건, 쿠폰 3건, 테스트유저(U1) 발급쿠폰 및 스탬프 적립)")
        } catch (e: Exception) {
            log.error("☕ [LocalDataInitializer] 시드 데이터 적재 중 오류 발생: {}", e.message, e)
        }
    }
}
