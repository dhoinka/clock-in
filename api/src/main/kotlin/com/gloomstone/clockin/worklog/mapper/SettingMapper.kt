package com.gloomstone.clockin.worklog.mapper

import com.gloomstone.clockin.worklog.domain.Setting
import com.gloomstone.clockin.worklog.dto.SettingResponse
import com.gloomstone.clockin.worklog.util.formatDuration
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import java.time.Duration


@Mapper(componentModel = "spring")
interface SettingMapper {
    @Mapping(
        target = "workingHours",
        qualifiedByName = ["format"]
    )
    @Mapping(
        target = "breakTime",
        qualifiedByName = ["format"]
    )
    fun toDto(setting: Setting): SettingResponse

    @Named("format")
    fun format(dur: Duration): String {
        return formatDuration(dur)
    }
}
