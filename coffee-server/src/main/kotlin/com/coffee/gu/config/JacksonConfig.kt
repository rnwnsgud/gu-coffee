package com.coffee.gu.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper

@Configuration
class JacksonConfig {

    @Bean
    fun jsonMapper(): JsonMapper {
        return JsonMapper.builder()
            .findAndAddModules()
            .build()
    }
}
