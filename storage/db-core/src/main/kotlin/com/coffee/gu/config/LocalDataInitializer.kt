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

        val (menuCount, ingredientCount, externalMenuMappingCount) = try {
            val menus = jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM menu") ?: 0L
            val ingredients = jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM ingredient") ?: 0L
            val mappings = jdbcTemplate.queryForObject<Long>("SELECT COUNT(*) FROM external_menu_mapping") ?: 0L
            Triple(menus, ingredients, mappings)
        } catch (e: Exception) {
            log.warn("☕ [LocalDataInitializer] 테이블 조회 실패 (테이블 미생성 등): {}", e.message)
            return
        }

        if (menuCount >= 6L && ingredientCount > 0L && externalMenuMappingCount > 0L) {
            log.info(
                "☕ [LocalDataInitializer] 이미 초기 데이터(메뉴 {}건, 원자재 {}건, 외부 메뉴 매핑 {}건)가 존재하므로 시드 데이터 투입을 건너뜁니다.",
                menuCount,
                ingredientCount,
                externalMenuMappingCount,
            )
            return
        }

        log.info("☕ [LocalDataInitializer] 로컬 개발/테스트용 초기 시드 데이터를 적재합니다...")

        try {
            val populator = ResourceDatabasePopulator(ClassPathResource("seed-data.sql"))
            populator.execute(dataSource)
            log.info("☕ [LocalDataInitializer] 초기 시드 데이터 적재 완료! (카테고리 4건, 메뉴 6건, 레시피 9건, 매장별 원자재 8건, 외부 메뉴 매핑 8건, 장바구니 1건, 쿠폰·스탬프 적립)")
        } catch (e: Exception) {
            log.error("☕ [LocalDataInitializer] 시드 데이터 적재 중 오류 발생: {}", e.message, e)
        }
    }
}
