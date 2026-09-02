package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.UserPrincipal
import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.worklog.dto.SettingResponse
import com.gloomstone.clockin.worklog.mapper.SettingMapper
import com.gloomstone.clockin.worklog.service.SettingService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SettingController(
    private val settingService: SettingService,
    private val settingMapper: SettingMapper,
    private val userService: UserService
) {
    @GetMapping("/settings")
    operator fun get(@AuthenticationPrincipal principal: UserPrincipal): SettingResponse {
        val setting = settingService.findByUser(principal.username)
        return settingMapper.toDto(setting)
    }

    @PutMapping("/settings")
    fun put(
        @RequestBody request: SettingResponse?,
        @AuthenticationPrincipal principal: UserPrincipal
    ): SettingResponse {
        request ?: throw BadRequestException()
        val user = userService.findByIdentity(principal.username) ?: throw BadRequestException("User not found")
        val setting = settingService.update(request, user)
        return settingMapper.toDto(setting)
    }
}