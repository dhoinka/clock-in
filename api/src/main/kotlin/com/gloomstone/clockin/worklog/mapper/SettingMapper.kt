package com.gloomstone.clockin.worklog.mapper

import com.gloomstone.clockin.worklog.domain.Setting
import com.gloomstone.clockin.worklog.dto.SettingResponse
import com.gloomstone.clockin.worklog.util.formatDuration
import org.springframework.stereotype.Component

@Component
class SettingMapper {
    fun toDto(setting: Setting) = SettingResponse(
        workingHours = formatDuration(setting.workingHours),
        breakTime = formatDuration(setting.breakTime),
        workingDays = setting.workingDays,
    )
}
