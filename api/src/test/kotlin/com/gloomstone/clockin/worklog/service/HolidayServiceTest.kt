package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.worklog.domain.Event
import com.gloomstone.clockin.worklog.domain.EventType
import com.gloomstone.clockin.worklog.domain.HolidaySync
import com.gloomstone.clockin.worklog.dto.Feiertag
import com.gloomstone.clockin.worklog.dto.HolidayDto
import com.gloomstone.clockin.worklog.dto.Land
import com.gloomstone.clockin.worklog.repository.EventRepository
import com.gloomstone.clockin.worklog.repository.HolidaySyncRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.web.client.RestTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate

class HolidayServiceTest {
    private val restTemplate = RestTemplate()
    private val eventRepository = mock<EventRepository>()
    private val syncRepository = mock<HolidaySyncRepository>()
    private val snapshotService = mock<SnapshotService>()
    private val transactionManager = mock<PlatformTransactionManager>()
    private val transactionStatus = mock<TransactionStatus>()
    private lateinit var mockServer: MockRestServiceServer
    private lateinit var service: HolidayService
    private val objectMapper = JsonMapper.builder().findAndAddModules().build()
    private var storedEvents = emptyList<Event>()
    private val syncedYears = mutableSetOf<Int>()

    @BeforeEach
    fun setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate)
        storedEvents = emptyList()
        syncedYears.clear()
        whenever(transactionManager.getTransaction(any<TransactionDefinition>())).thenReturn(transactionStatus)
        whenever(syncRepository.existsById(any())).thenAnswer { it.getArgument<Int>(0) in syncedYears }
        whenever(syncRepository.save(any<HolidaySync>())).thenAnswer {
            it.getArgument<HolidaySync>(0).also { sync -> syncedYears.add(sync.year) }
        }
        whenever(
            eventRepository.findAllByTypeAndStartBetweenOrderByStart(
                any(),
                any(),
                any(),
            ),
        ).thenAnswer { storedEvents }
        whenever(eventRepository.saveAll<Event>(any())).thenAnswer {
            it.getArgument<Iterable<Event>>(0).toList().also { events -> storedEvents = events }
        }
        service = HolidayService(restTemplate, eventRepository, syncRepository, snapshotService, transactionManager)
    }

    @Test
    fun `maps holidays applicable in Rhineland-Palatinate`() {
        val response =
            listOf(HolidayDto(LocalDate.of(2022, 1, 1), Feiertag("New Year", listOf(Land("Rheinland-Pfalz", "RP")))))
        mockServer.expect(requestTo("https://www.spiketime.de/feiertagapi/feiertage/2022"))
            .andRespond(
                withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(response))
            )

        val loaded = service.ensureYearLoaded(2022)

        mockServer.verify()
        assertThat(loaded).isTrue()
        assertThat(storedEvents.single())
            .extracting("title", "type", "start")
            .containsExactly("New Year", EventType.HOLIDAY, LocalDate.of(2022, 1, 1))
        verify(syncRepository).save(any<HolidaySync>())
        verify(snapshotService).invalidateFrom(LocalDate.of(2022, 1, 1))
    }

    @Test
    fun `fetches and persists a year only once`() {
        mockServer.expect(requestTo("https://www.spiketime.de/feiertagapi/feiertage/2026"))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("[]"))

        assertThat(service.ensureYearLoaded(2026)).isTrue()
        assertThat(service.ensureYearLoaded(2026)).isTrue()

        mockServer.verify()
        assertThat(syncedYears).containsExactly(2026)
    }

    @Test
    fun `failed fetch is not marked as synchronized`() {
        mockServer.expect(requestTo("https://www.spiketime.de/feiertagapi/feiertage/2027"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        assertThat(service.ensureYearLoaded(2027)).isFalse()

        mockServer.verify()
        assertThat(syncedYears).doesNotContain(2027)
        verify(syncRepository, never()).save(any<HolidaySync>())
    }
}
