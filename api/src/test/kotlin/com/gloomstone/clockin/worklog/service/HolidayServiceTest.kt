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
    fun beforeEach() {
        mockServer = MockRestServiceServer.createServer(restTemplate)
    }

    @Test
    fun test() {
        val holidayDto = listOf(
            HolidayDto(
                LocalDate.of(2022, 1, 1),
                Feiertag("New Year", listOf(Land("Rheinland-Pfalz", "RP")))
            )
        )

        mockServer.expect(requestTo("https://www.spiketime.de/feiertagapi/feiertage/2022"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(holidayDto))
            )

        val holidayService = HolidayService(restTemplate)
        val holidays = holidayService.getHolidays(LocalDate.of(2022, 1, 1))
        mockServer.verify()


        assertThat(holidays.size).isEqualTo(1)
        assertThat(holidays[0].name).isEqualTo("New Year")
        assertThat(holidays[0].isRp).isEqualTo(true)

    }

}
