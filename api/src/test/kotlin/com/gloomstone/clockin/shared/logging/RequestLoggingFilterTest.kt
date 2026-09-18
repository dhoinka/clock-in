package com.gloomstone.clockin.shared.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@ExtendWith(OutputCaptureExtension::class)
class RequestLoggingFilterTest {
    private val filter = RequestLoggingFilter()

    @Test
    fun `logs the completed request with status and duration`(output: CapturedOutput) {
        val request = MockHttpServletRequest("GET", "/workdays")
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, servletResponse ->
            (servletResponse as HttpServletResponse).status = 204
        }

        filter.doFilter(request, response, chain)

        assertThat(output.out).containsPattern("GET /workdays -> 204 \\(\\d+ ms\\)")
    }
}
