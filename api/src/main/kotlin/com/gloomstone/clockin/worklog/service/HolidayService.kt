package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.worklog.domain.*
import com.gloomstone.clockin.worklog.dto.HolidayDto
import com.gloomstone.clockin.worklog.repository.EventRepository
import com.gloomstone.clockin.worklog.repository.HolidaySyncRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

@Service
class HolidayService(
    private val restTemplate: RestTemplate,
    private val eventRepository: EventRepository,
    private val syncRepository: HolidaySyncRepository,
    private val snapshotService: SnapshotService,
    transactionManager: PlatformTransactionManager,
) {
    private val logger = LoggerFactory.getLogger(HolidayService::class.java)
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    @Synchronized
    fun ensureYearLoaded(year: Int): Boolean {
        if (syncRepository.existsById(year)) {
            return true
        }

        return try {
            logger.info("Fetching holidays, year={}", year)

            val feiertageResp =
                restTemplate.getForEntity<Array<HolidayDto>>(HOLIDAY_URL + year)

            val holidays = feiertageResp.body
                ?.map(::toHoliday)
                ?.filter { it.isRp || it.isAllStates }
                ?: emptyList()

            transactionTemplate.execute {
                persistHolidays(year, holidays)
            }

            logger.info("Persisted holidays, year={} count={}", year, holidays.size)
            true

        } catch (e: Exception) {
            logger.error("Failed to fetch holidays, year={}; the year remains eligible for retry", year, e)
            false
        }
    }

    private fun persistHolidays(year: Int, holidays: List<Holiday>) {
        // A second check also protects against another importer committing while the HTTP request was in flight.
        if (syncRepository.existsById(year)) {
            return
        }

        val start = LocalDate.of(year, 1, 1)
        val end = start.with(TemporalAdjusters.lastDayOfYear())
        val staleHolidays = eventRepository.findAllByTypeAndStartBetweenOrderByStart(EventType.HOLIDAY, start, end)
        if (staleHolidays.isNotEmpty()) {
            eventRepository.deleteAll(staleHolidays)
        }
        eventRepository.saveAll(holidays.map(::toEvent))
        syncRepository.save(HolidaySync(year, LocalDateTime.now()))
        snapshotService.invalidateFrom(start)
    }

    private fun toEvent(holiday: Holiday) = Event(
        title = holiday.name,
        type = EventType.HOLIDAY,
        start = holiday.date,
        end = holiday.date,
        isAllDay = true,
        status = EventStatus.APPROVED,
    )

    private fun toHoliday(dto: HolidayDto): Holiday {
        val date = dto.datum ?: throw BadRequestException("date is required")
        val name = dto.feiertag?.name ?: throw BadRequestException("name is required")

        val holiday = Holiday(date, name)

        val allStates = dto.feiertag?.laender?.size == 16
        holiday.isAllStates = allStates
        dto.feiertag?.laender?.forEach {
            when (it.abkuerzung) {
                "BW" -> holiday.isBw = true
                "BY" -> holiday.isBy = true
                "BE" -> holiday.isBe = true
                "BB" -> holiday.isBb = true
                "HB" -> holiday.isHb = true
                "HH" -> holiday.isHh = true
                "HE" -> holiday.isHe = true
                "MV" -> holiday.isMv = true
                "NW" -> holiday.isNw = true
                "RP" -> holiday.isRp = true
                "SL" -> holiday.isSl = true
                "SN" -> holiday.isSn = true
                "ST" -> holiday.isSt = true
                "SH" -> holiday.isSh = true
                "TH" -> holiday.isTh = true
                "NI" -> holiday.isNi = true
            }
        }
        return holiday
    }


    companion object {
        const val HOLIDAY_URL = "https://www.spiketime.de/feiertagapi/feiertage/"
    }
}
