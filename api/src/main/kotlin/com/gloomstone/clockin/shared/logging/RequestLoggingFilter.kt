package com.gloomstone.clockin.shared.logging

import com.google.common.base.Stopwatch
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

        val sw = Stopwatch.createStarted()

        try {
            filterChain.doFilter(request, response)
        } finally {
            sw.stop()
            requestLogger.info(
                "{} {} -> {} ({})",
                request.method,
                request.requestURI,
                response.status,
                sw.toString(),
            )
        }
    }
}
