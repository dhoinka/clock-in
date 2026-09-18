package com.gloomstone.clockin.shared.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RequestLoggingFilter : OncePerRequestFilter() {
    private val requestLogger = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val startedAt = System.nanoTime()

        try {
            filterChain.doFilter(request, response)
        } finally {
            val durationMs = (System.nanoTime() - startedAt) / 1_000_000
            requestLogger.info(
                "{} {} -> {} ({} ms)",
                request.method,
                request.requestURI,
                response.status,
                durationMs,
            )
        }
    }
}
