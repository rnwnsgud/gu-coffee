package com.coffee.gu.support.event

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.sql.DataSource

class PureSpringTransactionDemo {

    companion object {
        private val log = LoggerFactory.getLogger(PureSpringTransactionDemo::class.java)
    }

    @Test
    fun runDemo() {
        val context = AnnotationConfigApplicationContext(DemoConfig::class.java)

        log.info("================================================================")
        log.info("데모 시작: [순수 스프링] 동기 vs 비동기(@Async) 트랜잭션 리스너 동작 비교")
        log.info("================================================================")

        val demoService = context.getBean(DemoService::class.java)
        demoService.executeWithTransaction()

        Thread.sleep(2000)
        context.close()
    }

    @Configuration
    @EnableAsync
    @EnableTransactionManagement
    class DemoConfig {
        @Bean
        fun dataSource(): DataSource {
            return EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build()
        }

        @Bean
        fun transactionManager(dataSource: DataSource): PlatformTransactionManager {
            return DataSourceTransactionManager(dataSource)
        }

        @Bean(name = ["stampAsyncExecutor"])
        fun stampAsyncExecutor(): Executor {
            return Executors.newFixedThreadPool(2) { r ->
                val t = Thread(r)
                t.name = "CustomAsync-Thread"
                t
            }
        }

        @Bean
        fun demoService(eventPublisher: ApplicationEventPublisher): DemoService {
            return DemoService(eventPublisher)
        }

        @Bean
        fun demoListener(): DemoListener {
            return DemoListener()
        }
    }

    @Component
    class DemoService(private val eventPublisher: ApplicationEventPublisher) {

        @Transactional
        fun executeWithTransaction() {
            val threadName = Thread.currentThread().name
            log.info(
                "[메인 스레드] 시작 - Thread: {}, Transaction Active: {}",
                threadName, TransactionSynchronizationManager.isActualTransactionActive()
            )

            eventPublisher.publishEvent(DemoEvent("Hello Transaction"))

            log.info("[메인 스레드] 이벤트 발행 완료 - 곧 트랜잭션 커밋 예정")
        }
    }

    data class DemoEvent(val message: String)

    @Component
    class DemoListener {

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        fun handleSync(event: DemoEvent) {
            val threadName = Thread.currentThread().name
            val txActive = TransactionSynchronizationManager.isActualTransactionActive()
            log.info(
                "[동기 리스너] 호출됨 - Thread: {}, Transaction Active: {} (AFTER_COMMIT 이므로 false 예상)",
                threadName, txActive
            )
        }

        @Async("stampAsyncExecutor")
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        fun handleAsync(event: DemoEvent) {
            val threadName = Thread.currentThread().name
            val txActive = TransactionSynchronizationManager.isActualTransactionActive()
            log.info(
                "[비동기 @Async 리스너] 호출됨 - Thread: {}, Transaction Active: {} (새로운 트랜잭션이 시작됨)",
                threadName, txActive
            )
        }
    }
}
