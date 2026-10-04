package com.coffee.gu.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.conditions.ArchConditions.be
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchitectureTest {

    private lateinit var importedClasses: JavaClasses

    // 각 모듈의 소스 위치 판별 Predicate (non-null Boolean 반환으로 플랫폼 타입 경고 제거)
    private val resideInCoreDomain = DescribedPredicate.describe<JavaClass>("reside in core-domain module") { javaClass ->
        val uri = javaClass.source.orElse(null)?.uri?.toString() ?: ""
        uri.contains("core/core-domain") || uri.contains("core-domain")
    }

    private val resideInCoreApi = DescribedPredicate.describe<JavaClass>("reside in core-api module") { javaClass ->
        val uri = javaClass.source.orElse(null)?.uri?.toString() ?: ""
        uri.contains("core/core-api") || uri.contains("core-api")
    }

    private val resideInDbCore = DescribedPredicate.describe<JavaClass>("reside in storage:db-core module") { javaClass ->
        val uri = javaClass.source.orElse(null)?.uri?.toString() ?: ""
        uri.contains("storage/db-core") || uri.contains("db-core")
    }

    private val resideInAdminApi = DescribedPredicate.describe<JavaClass>("reside in admin-api module") { javaClass ->
        val uri = javaClass.source.orElse(null)?.uri?.toString() ?: ""
        uri.contains("admin-api")
    }

    @BeforeAll
    fun setup() {
        importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("com.coffee.gu")
    }

    @Test
    @DisplayName("[규칙 1] core-domain 모듈은 Spring 및 JPA/Hibernate 프레임워크에 전혀 의존하지 않아야 한다 (순수 도메인 원칙)")
    fun coreDomainShouldNotDependOnSpringOrJpa() {
        noClasses()
            .that(resideInCoreDomain)
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate.."
            )
            .because("도메인 모델과 도메인 Repository 인터페이스는 특정 프레임워크나 ORM 기술에 종속되지 않는 순수한 코틀린 클래스여야 합니다.")
            .check(importedClasses)
    }

    @Test
    @DisplayName("[규칙 2] core-api 모듈은 storage:db-core 모듈의 구현체에 직접 의존하지 않아야 한다 (DIP 원칙)")
    fun coreApiShouldNotDependOnDbCore() {
        noClasses()
            .that(resideInCoreApi)
            .should().dependOnClassesThat(resideInDbCore)
            .because("core-api는 DIP(의존 역전 원칙)에 따라 오직 core-domain의 Repository 인터페이스만 참조해야 하며, db-core의 JPA 구현체나 Entity를 직접 의존할 수 없습니다.")
            .check(importedClasses)
    }

    @Test
    @DisplayName("[규칙 3] core-domain 모듈은 상위 계층인 core-api, storage:db-core에 의존하지 않아야 한다 (단방향 의존성)")
    fun coreDomainShouldNotDependOnUpperModules() {
        noClasses()
            .that(resideInCoreDomain)
            .should().dependOnClassesThat(resideInCoreApi.or(resideInDbCore))
            .because("도메인 레이어는 시스템의 핵심 비즈니스 로직을 담는 최하위 순수 계층이므로 상위 계층인 API나 인프라 레이어를 역참조할 수 없습니다.")
            .check(importedClasses)
    }

    @Test
    @DisplayName("[규칙 4] JPA 엔티티(@Entity) 및 JpaRepository는 storage:db-core 및 admin-api 내부에만 위치해야 한다 (인프라 은닉)")
    fun jpaEntitiesAndRepositoriesShouldResideOnlyInStorage() {
        classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should(be(resideInDbCore.or(resideInAdminApi)))
            .because("JPA Entity는 데이터 영속성 계층(db-core, admin-api)에만 은닉되어야 하며 core-domain이나 core-api에 선언되어서는 안 됩니다.")
            .check(importedClasses)
    }

    @Test
    @DisplayName("[규칙 5] core-api의 Service 계층은 db-core의 Entity나 JpaRepository를 참조하거나 노출하지 않아야 한다")
    fun servicesShouldNotExposeOrDependOnEntities() {
        noClasses()
            .that(resideInCoreApi).and().haveSimpleNameEndingWith("Service")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Entity")
            .orShould().dependOnClassesThat().haveSimpleNameEndingWith("JpaRepository")
            .because("Service 계층은 Domain Model과 Domain Repository 인터페이스만을 다루어야 합니다.")
            .check(importedClasses)
    }

    @Test
    @DisplayName("[규칙 6] 패키지 간 순환 참조가 존재하지 않아야 한다 (Cycle-Free Architecture)")
    fun packagesShouldBeFreeOfCycles() {
        slices().matching("com.coffee.gu.(*)..")
            .should().beFreeOfCycles()
            .because("하위 패키지 간의 상호 순환 참조는 유지보수성과 확장성을 저해합니다.")
            .check(importedClasses)
    }
}
