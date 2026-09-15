package com.coffee.gu.event

import com.coffee.gu.Event
import com.coffee.gu.EventLogRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.lang.reflect.Modifier

class EventLogRepositoryArchitectureTest {

    @Test
    @DisplayName("아키텍처 검증: EventLogRepository의 모든 구현체는 publish(Event) 메서드에 @Transactional(propagation = Propagation.MANDATORY)이 필수 적용되어야 한다")
    fun verifyEventLogRepositoryImplementationsHaveMandatoryTransaction() {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AssignableTypeFilter(EventLogRepository::class.java))

        val candidateComponents = scanner.findCandidateComponents("com.coffee.gu")

        assertThat(candidateComponents).isNotEmpty

        for (bd in candidateComponents) {
            val clazz = Class.forName(bd.beanClassName)
            if (clazz.isInterface || Modifier.isAbstract(clazz.modifiers)) {
                continue
            }

            val publishMethod = clazz.getMethod("publish", Event::class.java)
            val transactional = publishMethod.getAnnotation(Transactional::class.java)

            if (transactional == null) {
                fail<Unit>("아키텍처 규칙 위반! 구현 클래스 [${clazz.name}]의 publish(Event) 메서드에 @Transactional 어노테이션이 누락되었습니다.")
            }

            if (transactional.propagation != Propagation.MANDATORY) {
                fail<Unit>("아키텍처 규칙 위반! 구현 클래스 [${clazz.name}]의 publish(Event) 메서드 트랜잭션 전파 속성이 MANDATORY가 아닙니다. (현재: ${transactional.propagation})")
            }
        }
    }
}
