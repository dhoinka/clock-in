package com.gloomstone.clockin.shared.testutil

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcConfigurer
import org.springframework.web.context.WebApplicationContext

class TestConfigHelpersTest {

    @Test
    fun `token(username) adds Authorization header`() {
        val processor: RequestPostProcessor = token("alice")
        val request = MockHttpServletRequest()
        processor.postProcessRequest(request)

        val header = request.getHeader("Authorization")
        assertThat(header).isNotNull
        assertThat(header).startsWith("Bearer ")
    }

    @Test
    fun `token full overload produces valid header`() {
        val processor = token(
            username = "bob",
            email = "bob@example.org",
            name = "Bob",
            roles = listOf("user")
        )

        val request = MockHttpServletRequest()
        processor.postProcessRequest(request)

        val header = request.getHeader("Authorization")
        assertThat(header).isNotNull
        assertThat(header).startsWith("Bearer ")
    }

    @Test
    fun `jwtHeader returns MockMvcConfigurer that adds token header when building mockMvc`() {
        // Mock a ConfigurableMockMvcBuilder to avoid loading Spring MVC classes at runtime
        val builder = Mockito.mock(ConfigurableMockMvcBuilder::class.java) as ConfigurableMockMvcBuilder<*>
        val configurer: MockMvcConfigurer = jwtHeader()

        // Use Mockito to provide a WebApplicationContext mock for the hook
        val mockContext = Mockito.mock(WebApplicationContext::class.java)
        val requestProcessor = configurer.beforeMockMvcCreated(builder, mockContext)

        val request = MockHttpServletRequest()
        requestProcessor?.postProcessRequest(request)

        val header = request.getHeader("Authorization")
        assertThat(header).isNotNull
        assertThat(header).startsWith("Bearer ")
    }
}
