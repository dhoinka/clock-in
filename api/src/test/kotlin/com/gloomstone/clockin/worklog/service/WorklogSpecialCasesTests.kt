package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.repository.UserRepository
import com.gloomstone.clockin.worklog.domain.EntryType
import com.gloomstone.clockin.worklog.domain.TimeEntry
import com.gloomstone.clockin.worklog.domain.Workday
import com.gloomstone.clockin.worklog.repository.DayRepository
import com.gloomstone.clockin.worklog.repository.TimeEntryRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import java.time.LocalDate

@DataJpaTest
class WorklogSpecialCasesTests {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var dayRepository: DayRepository

    @Autowired
    lateinit var timeEntryRepository: TimeEntryRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun `persists correction entries with their workday and user`() {
        val user = userRepository.save(User("alice@example.org", "alice", "Alice", id = "alice-id"))
        val date = LocalDate.of(2026, 9, 3)
        val workday = dayRepository.save(Workday(date, user))
        timeEntryRepository.save(TimeEntry("-8h", EntryType.CORRECTION, workday, user))
        entityManager.flush()
        entityManager.clear()

        val reloaded = requireNotNull(dayRepository.findByDateAndUser(date, userRepository.findById(user.id)))

        val entry = reloaded.entries.single()
        assertThat(entry.type).isEqualTo(EntryType.CORRECTION)
        assertThat(entry.duration).isEqualTo("-8h")
        assertThat(entry.user?.id).isEqualTo(user.id)
        assertThat(entry.workday?.id).isEqualTo(reloaded.id)
    }
}
