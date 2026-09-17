package com.gloomstone.clockin.worklog.repository

import com.gloomstone.clockin.worklog.domain.HolidaySync
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HolidaySyncRepository : JpaRepository<HolidaySync, Int>
