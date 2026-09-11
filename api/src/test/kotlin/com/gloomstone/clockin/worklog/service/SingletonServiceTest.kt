package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.worklog.domain.Setting
import com.gloomstone.clockin.worklog.domain.Snapshot
import com.gloomstone.clockin.worklog.repository.SettingRepository
import com.gloomstone.clockin.worklog.repository.SnapshotRepository
import com.gloomstone.clockin.worklog.repository.DayRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import java.util.Optional

class SingletonServiceTest {
    private val settingRepository: SettingRepository = mock()
    private val snapshotRepository: SnapshotRepository = mock()
    private val dayRepository: DayRepository = mock()

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

        SnapshotService(snapshotRepository, dayRepository).delete()

        assertThat(snapshot.id).isEqualTo(1)
        assertThat(snapshot.workday).isNull()
        verify(snapshotRepository).save(snapshot)
    }
}
