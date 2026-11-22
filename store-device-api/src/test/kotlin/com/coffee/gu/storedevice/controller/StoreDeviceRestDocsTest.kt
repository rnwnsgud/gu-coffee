package com.coffee.gu.storedevice.controller

import com.coffee.gu.api.controller.ApiControllerAdvice
import com.coffee.gu.storedevice.auth.StoreDeviceAuthenticator
import com.coffee.gu.storedevice.auth.StoreDevicePrincipalArgumentResolver
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.epages.restdocs.apispec.ResourceSnippetParameters
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler
import org.springframework.restdocs.snippet.Snippet
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder

@ExtendWith(RestDocumentationExtension::class)
abstract class StoreDeviceRestDocsTest {

    lateinit var mockMvc: MockMvc

    protected abstract val controller: Any
    protected abstract val storeDeviceAuthenticator: StoreDeviceAuthenticator

    @BeforeEach
    fun setUp(restDocumentation: RestDocumentationContextProvider) {
        val builder = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(StoreDevicePrincipalArgumentResolver(storeDeviceAuthenticator))
            .setControllerAdvice(ApiControllerAdvice())
        builder.apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
        this.mockMvc = builder.build()
    }

    fun document(
        identifier: String,
        tag: String,
        summary: String? = null,
        description: String? = null,
        vararg snippets: Snippet,
    ): RestDocumentationResultHandler {
        val resourceSnippet = ResourceSnippetParameters.builder()
            .tag(tag)
            .apply {
                summary?.let { summary(it) }
                description?.let { description(it) }
            }
        return MockMvcRestDocumentationWrapper.document(
            identifier = identifier,
            resourceDetails = resourceSnippet,
            snippets = snippets,
        )
    }
}
