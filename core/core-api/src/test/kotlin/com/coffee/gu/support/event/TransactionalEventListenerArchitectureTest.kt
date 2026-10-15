package com.coffee.gu.support.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import java.lang.reflect.Modifier

class TransactionalEventListenerArchitectureTest {

    @Test
    @DisplayName("아키텍처 검증: 모든 @TransactionalEventListener 메서드는 재시도 스케줄러 환경에서의 이벤트 유실을 방지하기 위해 fallbackExecution = true 가 필수 적용되어야 한다")
    fun verifyAllTransactionalEventListenersHaveFallbackExecutionTrue() {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AnnotationTypeFilter(Component::class.java))

        val candidateComponents = scanner.findCandidateComponents("com.coffee.gu")
        assertThat(candidateComponents).isNotEmpty

        val violatedMethods = mutableListOf<String>()

        for (bd in candidateComponents) {
            val clazz = Class.forName(bd.beanClassName)
            if (clazz.isInterface || Modifier.isAbstract(clazz.modifiers)) {
                continue
            }

            // 테스트 전용 클래스 및 데모 클래스는 검증 대상에서 제외 (프로덕션 코드 대상)
            val codeSource = clazz.protectionDomain?.codeSource?.location?.path ?: ""
            if (codeSource.contains("/classes/kotlin/test") || codeSource.contains("/classes/java/test") || clazz.name.contains("Test") || clazz.name.contains("Demo")) {
                continue
            }

            for (method in clazz.declaredMethods) {
                val annotation = method.getAnnotation(TransactionalEventListener::class.java) ?: continue

                if (!annotation.fallbackExecution) {
                    violatedMethods.add("${clazz.name}.${method.name}() (현재: fallbackExecution = false)")
                }
            }
        }

        if (violatedMethods.isNotEmpty()) {
            fail<Unit>(
                """
                아키텍처 규칙 위반!
                트랜잭션이 없는 비동기/재시도 스케줄러 환경에서 이벤트가 침묵 무시(Drop)되는 버그를 방지하기 위해,
                모든 @TransactionalEventListener 메서드는 반드시 fallbackExecution = true 를 명시해야 합니다.
                위반된 메서드 목록:
                ${violatedMethods.joinToString("\n")}
                """.trimIndent(),
            )
        }
    }
}
