package com.gloomstone.clockin.worklog.repository

import com.gloomstone.clockin.worklog.domain.Setting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SettingRepository : JpaRepository<Setting, Long>
