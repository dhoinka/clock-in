package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.worklog.dto.StatMapping
import com.gloomstone.clockin.worklog.service.StatService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminController(private val statService: StatService) {
    @GetMapping("/admin/stats")
    @PreAuthorize("hasAuthority('admin')")
    fun get(): Map<String, MutableList<StatMapping>> {
        return statService.getAdminStats()
    }
}
