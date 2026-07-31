package com.coffee.gu.support.event;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

//23:14:26.193 [Test worker] INFO org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory -- Starting embedded database: url='jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'
//23:14:26.290 [Test worker] INFO com.coffee.core.support.event.PureSpringTransactionDemo -- ================================================================
//23:14:26.290 [Test worker] INFO com.coffee.core.support.event.PureSpringTransactionDemo -- 데모 시작: [순수 스프링] 동기 vs 비동기(@Async) 트랜잭션 리스너 동작 비교
//23:14:26.290 [Test worker] INFO com.coffee.core.support.event.PureSpringTransactionDemo -- ================================================================
//23:14:26.292 [Test worker] INFO com.coffee.core.support.event.PureSpringTransactionDemo -- [메인 스레드] 시작 - Thread: Test worker, Transaction Active: true
//23:14:26.293 [Test worker] INFO com.coffee.core.support.event.PureSpringTransactionDemo -- [메인 스레드] 이벤트 발행 완료 - 곧 트랜잭션 커밋 예정
//23:14:26.295 [CustomAsync-Thread] INFO com.coffee.core.support.event.PureSpringTransactionDemo -- [비동기 @Async 리스너] 호출됨 - Thread: CustomAsync-Thread, Transaction Active: true (새로운 트랜잭션이 시작됨)
//23:14:26.295 [Test worker] INFO com.coffee.core.support.event.PureSpringTransactionDemo -- [동기 리스너] 호출됨 - Thread: Test worker, Transaction Active: true (AFTER_COMMIT 이므로 false 예상)
//23:14:28.302 [Test worker] INFO org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory -- Shutting down embedded database: url='jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false'
public class PureSpringTransactionDemo {

    private static final Logger log = LoggerFactory.getLogger(PureSpringTransactionDemo.class);

    @Test
    void runDemo() throws InterruptedException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DemoConfig.class);
        
        log.info("================================================================");
        log.info("데모 시작: [순수 스프링] 동기 vs 비동기(@Async) 트랜잭션 리스너 동작 비교");
        log.info("================================================================");

        DemoService demoService = context.getBean(DemoService.class);
        demoService.executeWithTransaction();

        Thread.sleep(2000);
        context.close();
    }

    @Configuration
    @EnableAsync
    @EnableTransactionManagement
    public static class DemoConfig {
        @Bean
        public DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .build();
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean(name = "stampAsyncExecutor")
        public Executor stampAsyncExecutor() {
            return Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r);
                t.setName("CustomAsync-Thread");
                return t;
            });
        }

        @Bean
        public DemoService demoService(ApplicationEventPublisher eventPublisher) {
            return new DemoService(eventPublisher);
        }

        @Bean
        public DemoListener demoListener() {
            return new DemoListener();
        }
    }

    @Component
    public static class DemoService {
        private final ApplicationEventPublisher eventPublisher;

        public DemoService(ApplicationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
        }

        @Transactional
        public void executeWithTransaction() {
            String threadName = Thread.currentThread().getName();
            String txName = TransactionSynchronizationManager.getCurrentTransactionName();
            log.info("[메인 스레드] 시작 - Thread: {}, Transaction Active: {}", threadName, TransactionSynchronizationManager.isActualTransactionActive());

            eventPublisher.publishEvent(new DemoEvent("Hello Transaction"));
            
            log.info("[메인 스레드] 이벤트 발행 완료 - 곧 트랜잭션 커밋 예정");
        }
    }

    public static record DemoEvent(String message) {}

    @Component
    public static class DemoListener {
        
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void handleSync(DemoEvent event) {
            String threadName = Thread.currentThread().getName();
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("[동기 리스너] 호출됨 - Thread: {}, Transaction Active: {} (AFTER_COMMIT 이므로 false 예상)", 
                    threadName, txActive);
        }

        @Async("stampAsyncExecutor")
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void handleAsync(DemoEvent event) {
            String threadName = Thread.currentThread().getName();
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("[비동기 @Async 리스너] 호출됨 - Thread: {}, Transaction Active: {} (새로운 트랜잭션이 시작됨)", 
                    threadName, txActive);
        }
    }
}
