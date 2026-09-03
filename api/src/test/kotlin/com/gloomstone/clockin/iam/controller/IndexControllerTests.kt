package com.gloomstone.clockin.iam.controller

import com.gloomstone.clockin.config.AppConfig
import com.gloomstone.clockin.config.security.JwtResourceServerConfig
import com.gloomstone.clockin.config.security.WebSecurityConfig
import com.gloomstone.clockin.shared.testutil.token
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(IndexController::class)
@Import(WebSecurityConfig::class, JwtResourceServerConfig::class, AppConfig::class)
class IndexControllerTests {
    @Autowired
    lateinit var mvc: MockMvc

    @Test
    fun testIndex() {
        mvc.perform(get("/").with(token()))
            .andExpect(status().isOk)
    }
}
