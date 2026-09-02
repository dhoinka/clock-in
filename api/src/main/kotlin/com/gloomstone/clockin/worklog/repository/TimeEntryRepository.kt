package com.gloomstone.clockin.worklog.repository

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.worklog.domain.EntryType
import com.gloomstone.clockin.worklog.domain.TimeEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TimeEntryRepository : JpaRepository<TimeEntry, Long> {

    fun findAllByTypeAndUser(type: EntryType, user: User): List<TimeEntry>

    @Query(
        """
        select _q.username, 
	        cast(avg(extract(hour from _q.start_ts) + (extract(minute from _q.start_ts) / 60)) as text) as start_ts, 
	        cast(avg(extract(hour from _q.end_ts) + (extract(minute from _q.end_ts) / 60)) as text) as end_ts
        from (
            select u.username , d.date, min(b.start_ts) as start_ts, max(b.end_ts) as end_ts
            from workday d inner join time_entry b on d.id = b.workday_id
            join users u on d.user_id = u.id
            where b.start_ts is not null and b.end_ts is not null
            and u.id = ?1
            group by d.date, u.username 
            order by d.date
        ) as _q
        group by _q.username
        ;
    """,
        nativeQuery = true
    )
    fun getStats(userId: String): Map<String, String>

    fun deleteAllByUser(user: User)
}
