package com.coffee.gu;

import com.coffee.gu.config.CoreDataSourceConfig;
import com.coffee.gu.config.CoreJpaConfig;
import com.coffee.gu.config.QuerydslConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestClient;

import java.util.concurrent.Executor;

@Import({CoreDataSourceConfig.class, CoreJpaConfig.class, QuerydslConfig.class})
@SpringBootApplication(scanBasePackages = "com.coffee.gu")
@ConfigurationPropertiesScan(basePackages = "com.coffee.gu")
@EnableJpaAuditing
@EnableAsync
@EnableTransactionManagement
@EntityScan(basePackages = "com.coffee.gu")
@EnableJpaRepositories(basePackages = "com.coffee.gu")
public class TestApplication {
    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

    @Bean(name = "stampAsyncExecutor")
    public Executor stampAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setThreadNamePrefix("StampAsync-");
        executor.initialize();
        return executor;
    }
}
