package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.worklog.domain.Holiday
import com.gloomstone.clockin.worklog.dto.HolidayDto
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

@Service
class HolidayService(private val restTemplate: RestTemplate) {
    private val logger = LoggerFactory.getLogger(HolidayService::class.java)

    @Cacheable(value = ["holidays"], key = "#date.year")
    fun getHolidays(date: LocalDate): List<Holiday> {
        return try {
            logger.info("get live holidays for year {}", date.year)

            val feiertageResp =
                restTemplate.getForEntity(HOLIDAY_URL + date.year, Array<HolidayDto>::class.java)

            feiertageResp.body?.map {
                this.toEntity(it)
            }?.filter { it.isRp || it.isAllStates } ?: emptyList()

        } catch (e: Exception) {
            logger.error("error during getHolidays {}", e.message)
            emptyList()
        }
    }

    private fun toEntity(dto: HolidayDto): Holiday {
        val date = dto.datum ?: throw BadRequestException("date is required")
        val name = dto.feiertag?.name ?: throw BadRequestException("name is required")

        val holiday = Holiday(date, name)

        val allStates = dto.feiertag?.laender?.size == 16
        holiday.isAllStates = allStates
        dto.feiertag?.laender?.forEach {
            when (it.abkuerzung!!) {
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
