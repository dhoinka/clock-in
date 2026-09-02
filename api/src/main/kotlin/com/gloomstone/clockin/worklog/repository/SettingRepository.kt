package com.gloomstone.clockin.worklog.repository

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.worklog.domain.Setting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SettingRepository : JpaRepository<Setting, Long> {
    fun findByUser(user: User): Setting?

    fun save(entity: Setting): Setting

    override fun delete(entity: Setting)
}