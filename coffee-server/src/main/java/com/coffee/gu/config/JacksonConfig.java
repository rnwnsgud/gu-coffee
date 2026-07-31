package com.coffee.gu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;


@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
//                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // default false/
                // WRITE_DATES_AS_TIMESTAMPS default ?
                .build();
    }

}
