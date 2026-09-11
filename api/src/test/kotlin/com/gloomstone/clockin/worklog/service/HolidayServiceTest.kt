package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.worklog.dto.Feiertag
import com.gloomstone.clockin.worklog.dto.HolidayDto
import com.gloomstone.clockin.worklog.dto.Land
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate

class HolidayServiceTest {
    private val restTemplate = RestTemplate()
    private lateinit var mockServer: MockRestServiceServer
    private val objectMapper = JsonMapper.builder().findAndAddModules().build()

    @BeforeEach
    fun setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate)
    }

    @Test
    fun `maps holidays applicable in Rhineland-Palatinate`() {
        val response = listOf(HolidayDto(LocalDate.of(2022, 1, 1), Feiertag("New Year", listOf(Land("Rheinland-Pfalz", "RP")))))
        mockServer.expect(requestTo("https://www.spiketime.de/feiertagapi/feiertage/2022"))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(objectMapper.writeValueAsString(response)))

        val holidays = HolidayService(restTemplate).getHolidays(LocalDate.of(2022, 1, 1))

        mockServer.verify()
        val holiday = holidays.single()
        assertThat(holiday.name).isEqualTo("New Year")
        assertThat(holiday.isRp).isTrue()
    }
}
