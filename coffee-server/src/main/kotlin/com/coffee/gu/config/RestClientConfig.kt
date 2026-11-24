package com.coffee.gu.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class RestClientConfig {

    @Bean
    @Primary
    fun restClient(): RestClient {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(3000)
            setReadTimeout(5000)
        }

        return RestClient.builder()
            .requestFactory(factory)
            .build()
    }

    @Bean("llmRestClient")
    fun llmRestClient(
        @Value("\${gemini.api.connect-timeout-seconds:5}") connectTimeoutSeconds: Long,
        @Value("\${gemini.api.read-timeout-seconds:60}") readTimeoutSeconds: Long,
    ): RestClient {
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
            .build()
        val factory = JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(Duration.ofSeconds(readTimeoutSeconds))
        }

        return RestClient.builder()
            .requestFactory(factory)
            .build()
    }
}
