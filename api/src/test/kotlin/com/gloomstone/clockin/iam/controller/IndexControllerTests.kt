package com.gloomstone.clockin.iam.controller

import com.gloomstone.clockin.shared.testutil.token
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class IndexControllerTests {
    @Autowired
    lateinit var mvc: MockMvc

    @Test
    fun testIndex() {
        mvc.perform(get("/").with(token()))
            .andExpect(status().isOk)
    }
}