package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.worklog.domain.Setting
import com.gloomstone.clockin.worklog.domain.Snapshot
import com.gloomstone.clockin.worklog.domain.Workday
import com.gloomstone.clockin.worklog.repository.SettingRepository
import com.gloomstone.clockin.worklog.repository.SnapshotRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Duration
import java.time.LocalDate
import java.util.*

class SingletonServiceTest {
    private val settingRepository: SettingRepository = mock()
    private val snapshotRepository: SnapshotRepository = mock()

    @Test
    fun `settings are created using fixed singleton id`() {
        whenever(settingRepository.findById(1)).thenReturn(Optional.empty())
        whenever(settingRepository.save(any<Setting>())).thenAnswer { it.arguments[0] }

        val setting = SettingService(settingRepository).get()

        assertThat(setting.id).isEqualTo(1)
        assertThat(setting.workingHours).isEqualTo(Duration.ofHours(8))
        verify(settingRepository).save(setting)
    }

    @Test
    fun `snapshot reset preserves the fixed singleton row`() {
        val snapshot = Snapshot()
        whenever(snapshotRepository.findById(1)).thenReturn(Optional.of(snapshot))

        SnapshotService(snapshotRepository).delete()

        assertThat(snapshot.id).isEqualTo(1)
        assertThat(snapshot.workday).isNull()
        verify(snapshotRepository).save(snapshot)
    }

    @Test
    fun `snapshot advances to the latest calculated day`() {
        val previousDay = Workday(LocalDate.of(2026, 9, 7))
        val latestDay = Workday(LocalDate.of(2026, 9, 8))
        val snapshot = Snapshot(previousDay)
        whenever(snapshotRepository.findById(1)).thenReturn(Optional.of(snapshot))

        SnapshotService(snapshotRepository).advanceTo(latestDay)

        assertThat(snapshot.workday).isSameAs(latestDay)
        verify(snapshotRepository).save(snapshot)
    }

    @Test
    fun `snapshot does not move backwards`() {
        val latestDay = Workday(LocalDate.of(2026, 9, 8))
        val snapshot = Snapshot(latestDay)
        whenever(snapshotRepository.findById(1)).thenReturn(Optional.of(snapshot))

        SnapshotService(snapshotRepository).advanceTo(Workday(LocalDate.of(2026, 9, 7)))

        assertThat(snapshot.workday).isSameAs(latestDay)
        verify(snapshotRepository, never()).save(snapshot)
    }

    @Test
    fun `change on snapshot day invalidates cached balance`() {
        val date = LocalDate.of(2026, 9, 8)
        val snapshot = Snapshot(Workday(date))
        whenever(snapshotRepository.findById(1)).thenReturn(Optional.of(snapshot))

        SnapshotService(snapshotRepository).invalidateFrom(date)

        assertThat(snapshot.workday).isNull()
        verify(snapshotRepository).save(snapshot)
    }
}
