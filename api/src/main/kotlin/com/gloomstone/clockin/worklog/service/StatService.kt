package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.iam.repository.UserRepository
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.exception.InternalServerException
import com.gloomstone.clockin.worklog.domain.Stat
import com.gloomstone.clockin.worklog.dto.StatMapping
import com.gloomstone.clockin.worklog.repository.StatRepository
import com.gloomstone.clockin.worklog.repository.TimeEntryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class StatService(
    private val timeEntryRepository: TimeEntryRepository,
    private val userRepository: UserRepository,
    private val statRepository: StatRepository,
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(WorklogService::class.java)

    fun getStats(username: String): Stat {
        val user = userService.findByIdentity(username) ?: throw InternalServerException("User not found")
        val userId = user.id
        val map = this.timeEntryRepository.getStats(userId)
        return if (map.isNotEmpty()) {
            try {
                val username = map["username"] ?: throw InternalServerException("Username missing")
                val startTs = map["start_ts"] ?: throw InternalServerException("Start ts missing")
                val endTs = map["end_ts"] ?: throw InternalServerException("End ts missing")

                Stat(username, BigDecimal(startTs).toDouble(), BigDecimal(endTs).toDouble())
            } catch (e: Exception) {

                logger.error("getStats failed: {}, {}", map.values, map.keys, e)
                Stat(user.username, 0.0, 0.0)
            }
        } else {
            Stat(user.username, 0.0, 0.0)
        }
    }

    fun getAdminStats(): Map<String, MutableList<StatMapping>> {
        val map = this.userRepository.findAll().associate { it.username to mutableListOf<StatMapping>() }
        val statList = this.statRepository.getStat()

        statList.forEach {
            map[it.username]?.add(it)
        }

        return map
    }

}
