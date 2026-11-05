package com.coffee.gu.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.security.MessageDigest

class FlywayMigrationImmutabilityTest {

    // 이미 배포/적용 완료된 마이그레이션 파일들의 SHA-256 해시값 (절대 변경 불가)
    private val appliedMigrationChecksums = mapOf(
        "V1__init_schema.sql" to "b0e66e3859c9b347a1a3c0b7e515abf592fd360a7076867b6221267d98504a6e",
        "V2__add_limited_coupon.sql" to "5cd7e04fe02741a1d442509626ebc2fa5e0057389b673afee5459daa3692b18c",
    )

    @Test
    @DisplayName("[무결성 검증] 이미 적용된 기존 Flyway 마이그레이션 파일(V1, V2)은 1바이트도 수정되어서는 안 된다")
    fun appliedMigrationsMustNeverBeModified() {
        val migrationDir = File("src/main/resources/db/migration")

        for ((fileName, expectedSha256) in appliedMigrationChecksums) {
            val file = File(migrationDir, fileName)
            assertThat(file.exists())
                .`as`("마이그레이션 파일 $fileName 이 존재해야 합니다.")
                .isTrue()

            val actualSha256 = calculateSha256(file)
            assertThat(actualSha256)
                .`as`(
                    """
                    [치명적 오류] 이미 배포/적용된 Flyway 마이그레이션 파일($fileName)이 수정되었습니다!
                    기존 마이그레이션 파일을 수정하면 DB 기동 시 'Migration checksum mismatch' 에러가 발생합니다.
                    반드시 .agents/rules/flyway_migration_hygiene.md 규칙에 따라 기존 파일 수정을 취소(git checkout)하고,
                    새로운 V{N+1}__*.sql 파일을 생성하여 스키마를 변경하십시오.
                    """.trimIndent()
                )
                .isEqualTo(expectedSha256)
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = file.readBytes()
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }
}
