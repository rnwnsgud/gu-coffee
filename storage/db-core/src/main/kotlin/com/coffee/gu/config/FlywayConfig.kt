package com.coffee.gu.config

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
@ConditionalOnProperty(name = ["spring.flyway.enabled"], havingValue = "true", matchIfMissing = true)
class FlywayConfig {

    @Bean
    fun flyway(@Qualifier("coreDataSource") dataSource: DataSource): Flyway {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .load()
        flyway.migrate()
        return flyway
    }

    @Bean
    fun flywayDependsOnPostProcessor(): BeanFactoryPostProcessor {
        return BeanFactoryPostProcessor { beanFactory: ConfigurableListableBeanFactory ->
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                val beanDef = beanFactory.getBeanDefinition("entityManagerFactory")
                val existing = beanDef.dependsOn ?: emptyArray()
                if (!existing.contains("flyway")) {
                    beanDef.setDependsOn(*existing, "flyway")
                }
            }
        }
    }
}
