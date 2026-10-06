package com.coffee.gu.api.controller

import com.coffee.gu.auth.PrincipalArgumentResolver
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler
import org.springframework.restdocs.snippet.Snippet
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@ExtendWith(RestDocumentationExtension::class)
abstract class RestDocsTest {

    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp(restDocumentation: RestDocumentationContextProvider) {
        val builder = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(PrincipalArgumentResolver())
            .setControllerAdvice(ApiControllerAdvice())
        builder.apply<org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
        this.mockMvc = builder.build()
    }

    protected abstract val controller: Any

    fun document(identifier: String, vararg snippets: Snippet): RestDocumentationResultHandler {
        return MockMvcRestDocumentationWrapper.document(
            identifier = identifier,
            snippets = snippets
        )
    }
}
