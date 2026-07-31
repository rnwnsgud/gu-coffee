package com.coffee.gu.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setQueueCapacity(200);
        executor.setMaxPoolSize(10);
        executor.setThreadNamePrefix("GlobalAsync-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "stampAsyncExecutor")
    public Executor stampAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // DB 작업 위주이므로 스레드를 조금 더 여유 있게 배정
        executor.setQueueCapacity(500);
        executor.setMaxPoolSize(20);
        executor.setThreadNamePrefix("StampAsync-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 3. 추가 스레드 풀 B: 카카오톡 알림톡 / FCM 푸시 알림 전용 풀 (외부 API 연동용)
     */
//    @Bean(name = "notificationAsyncExecutor")
//    public Executor notificationAsyncExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(3);
//        executor.setQueueCapacity(1000); // 알림은 밀려도 유실되면 안 되므로 큐를 크게 잡음
//        executor.setMaxPoolSize(5);
//        executor.setThreadNamePrefix("NotiAsync-");
//        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
//        executor.initialize();
//        return executor;
//    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("비동기 스레드 에러 발생 메서드명: {} | 에러 : {}", method.getName(), ex.getMessage());
        };
    }
}
