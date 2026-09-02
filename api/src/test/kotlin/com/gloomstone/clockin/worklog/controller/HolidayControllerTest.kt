package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.shared.testutil.token
import com.gloomstone.clockin.worklog.dto.HolidayResponse
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate

@SpringBootTest
@AutoConfigureMockMvc
class HolidayControllerTest {

    @Autowired
    lateinit var mvc: MockMvc

    @MockitoBean
    lateinit var restTemplate: RestTemplate

    @Test
    fun `test GET`() {
        val value = ResponseEntity.ok().build<HolidayResponse>()
        given(restTemplate.getForEntity(anyString(), eq(HolidayResponse::class.java))).willReturn(value)

        mvc.perform(get("/holidays").with(token()))
            .andExpect(status().isOk)

    }

}
