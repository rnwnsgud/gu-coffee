package com.coffee.gu.support.event;

import com.coffee.gu.TestApplication;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SpringBootTest(classes = TestApplication.class)
@Import({AsyncTransactionDemo.DemoService.class, AsyncTransactionDemo.DemoListener.class})
public class AsyncTransactionDemo {

    private static final Logger log = LoggerFactory.getLogger(AsyncTransactionDemo.class);

    @Autowired
    private DemoService demoService;

    @Test
    void runDemo() throws InterruptedException {
        log.info("================================================================");
        log.info("데모 시작: 동기 vs 비동기(@Async) 트랜잭션 리스너 동작 비교");
        log.info("================================================================");

        demoService.executeWithTransaction();

        // 비동기 로그 출력을 기다리기 위해 잠시 대기
        Thread.sleep(2000);
    }

    @SpringBootApplication(scanBasePackages = "com.coffee.gu.support.event")
    @EnableAsync
    @EnableTransactionManagement
    @Import({DemoService.class, DemoListener.class})
    public static class DemoConfig {
        
        @Bean
        @Primary
        public DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .build();
        }

        @Bean
        @Primary
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
            log.info("[메인 스레드] 시작 - Thread: {}, Transaction: {}", threadName, txName);

            eventPublisher.publishEvent(new DemoEvent("Hello Transaction"));
            
            log.info("[메인 스레드] 이벤트 발행 완료 - 곧 트랜잭션 커밋 예정");
        }
    }

    public record DemoEvent(String message) {}

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
            String txName = TransactionSynchronizationManager.getCurrentTransactionName();
            log.info("[비동기 @Async 리스너] 호출됨 - Thread: {}, Transaction Active: {}, Transaction Name: {}", 
                    threadName, txActive, txName);
        }
    }
}
