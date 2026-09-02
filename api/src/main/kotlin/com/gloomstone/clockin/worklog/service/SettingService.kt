package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.worklog.domain.Setting
import com.gloomstone.clockin.worklog.dto.SettingResponse
import com.gloomstone.clockin.worklog.repository.SettingRepository
import com.gloomstone.clockin.worklog.util.toDuration
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
class SettingService(
    private val settingRepository: SettingRepository,
    private val userService: UserService
) {

    fun findByUser(user: User): Setting {
        return settingRepository.findByUser(user) ?: createSetting(user)
    }

    fun findByUser(username: String): Setting {
        val user = userService.findByIdentity(username) ?: throw BadRequestException("User not found")
        return findByUser(user)
    }

    @Transactional
    fun update(request: SettingResponse, user: User): Setting {
        val setting = findByUser(user)

        val workingHours = request.workingHours.toDuration()
        setting.workingHours = workingHours ?: throw BadRequestException("Invalid working hours")

        val breakTime = request.breakTime.toDuration()
        setting.breakTime = breakTime ?: throw BadRequestException("Invalid break time")

        setting.workingDays = request.workingDays
        return settingRepository.save(setting)
    }

    private fun createSetting(user: User): Setting {
        return Setting(DEFAULT_WORKING_HOURS, DEFAULT_BREAK_TIME, DEFAULT_WORKING_DAYS, user)
    }

    companion object {
        private val DEFAULT_WORKING_HOURS = Duration.ofHours(8)
        private val DEFAULT_BREAK_TIME = Duration.ofMinutes(30)
        private const val DEFAULT_WORKING_DAYS = 31L
    }


}
