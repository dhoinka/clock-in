package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.worklog.domain.Setting
import com.gloomstone.clockin.worklog.dto.SettingResponse
import com.gloomstone.clockin.worklog.repository.SettingRepository
import com.gloomstone.clockin.worklog.util.toDuration
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
class SettingService(private val repository: SettingRepository) {
    fun get(): Setting = repository.findById(1).orElseGet {
        repository.save(Setting(Duration.ofHours(8), Duration.ofMinutes(30), 31L))
    }

    @Transactional
    fun update(request: SettingResponse): Setting {
        val setting = get()
        setting.workingHours = request.workingHours.toDuration() ?: throw BadRequestException("Invalid working hours")
        setting.breakTime = request.breakTime.toDuration() ?: throw BadRequestException("Invalid break time")
        setting.workingDays = request.workingDays
        return repository.save(setting)
    }
}
