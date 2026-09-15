package com.coffee.gu.support.event

import com.coffee.gu.TestApplication
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
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

@SpringBootTest(classes = [TestApplication::class])
@Import(AsyncTransactionDemo.DemoService::class, AsyncTransactionDemo.DemoListener::class)
class AsyncTransactionDemo {

    companion object {
        private val log = LoggerFactory.getLogger(AsyncTransactionDemo::class.java)
    }

    @Autowired
    private lateinit var demoService: DemoService

    @Test
    fun runDemo() {
        log.info("================================================================")
        log.info("데모 시작: 동기 vs 비동기(@Async) 트랜잭션 리스너 동작 비교")
        log.info("================================================================")

        demoService.executeWithTransaction()

        Thread.sleep(2000)
    }

    @SpringBootApplication(scanBasePackages = ["com.coffee.gu.support.event"])
    @EnableAsync
    @EnableTransactionManagement
    @Import(DemoService::class, DemoListener::class)
    class DemoConfig {

        @Bean
        @Primary
        fun dataSource(): DataSource {
            return EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build()
        }

        @Bean
        @Primary
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
    }

    @Component
    class DemoService(private val eventPublisher: ApplicationEventPublisher) {

        @Transactional
        fun executeWithTransaction() {
            val threadName = Thread.currentThread().name
            val txName = TransactionSynchronizationManager.getCurrentTransactionName()
            log.info("[메인 스레드] 시작 - Thread: {}, Transaction: {}", threadName, txName)

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
            val txName = TransactionSynchronizationManager.getCurrentTransactionName()
            log.info(
                "[비동기 @Async 리스너] 호출됨 - Thread: {}, Transaction Active: {}, Transaction Name: {}",
                threadName, txActive, txName
            )
        }
    }
}
