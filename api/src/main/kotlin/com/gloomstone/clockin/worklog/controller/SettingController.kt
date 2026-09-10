package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.worklog.dto.SettingResponse
import com.gloomstone.clockin.worklog.mapper.SettingMapper
import com.gloomstone.clockin.worklog.service.SettingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SettingController(
    private val service: SettingService,
    private val mapper: SettingMapper,
) {
    @GetMapping("/settings")
    fun get() = mapper.toDto(service.get())

    @PutMapping("/settings")
    fun put(@RequestBody request: SettingResponse) = mapper.toDto(service.update(request))
}
