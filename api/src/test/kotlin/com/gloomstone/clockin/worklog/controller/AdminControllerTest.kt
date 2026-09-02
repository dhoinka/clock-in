package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.shared.testutil.token
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AdminControllerTest {

    @Autowired
    lateinit var mvc: MockMvc

    @Test
    fun test() {
        this.mvc.perform(get("/admin/stats").with(token()))
            .andExpect(status().isOk)
    }
}