package com.coffee.gu.webhook.controller

import com.coffee.gu.api.controller.ApiControllerAdvice
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
abstract class WebhookRestDocsTest {

    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp(restDocumentation: RestDocumentationContextProvider) {
        val builder = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(ApiControllerAdvice())
        builder.apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
        this.mockMvc = builder.build()
    }

    protected abstract val controller: Any

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
