package com.coffee.gu.event;

import com.coffee.gu.Event;
import com.coffee.gu.EventLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class EventLogRepositoryArchitectureTest {

    @Test
    @DisplayName("아키텍처 검증: EventLogRepository의 모든 구현체는 publish(Event) 메서드에 @Transactional(propagation = Propagation.MANDATORY)이 필수 적용되어야 한다")
    void verifyEventLogRepositoryImplementationsHaveMandatoryTransaction() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(EventLogRepository.class));

        Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents("com.coffee.gu");

        assertThat(candidateComponents).isNotEmpty();

        for (BeanDefinition bd : candidateComponents) {
            Class<?> clazz = Class.forName(bd.getBeanClassName());
            if (clazz.isInterface() || java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            Method publishMethod = clazz.getMethod("publish", Event.class);
            Transactional transactional = publishMethod.getAnnotation(Transactional.class);

            if (transactional == null) {
                fail(String.format("아키텍처 규칙 위반! 구현 클래스 [%s]의 publish(Event) 메서드에 @Transactional 어노테이션이 누락되었습니다.", clazz.getName()));
            }

            if (transactional.propagation() != Propagation.MANDATORY) {
                fail(String.format("아키텍처 규칙 위반! 구현 클래스 [%s]의 publish(Event) 메서드 트랜잭션 전파 속성이 MANDATORY가 아닙니다. (현재: %s)", clazz.getName(), transactional.propagation()));
            }
        }
    }
}
