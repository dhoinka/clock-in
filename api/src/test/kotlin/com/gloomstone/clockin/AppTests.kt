package com.gloomstone.clockin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import javax.sql.DataSource
import org.springframework.beans.factory.annotation.Autowired

@SpringBootTest
class AppTests {
    @Autowired
    lateinit var dataSource: DataSource

    @Test
    fun contextLoads() {
    }

    @Test
    fun `fresh schema contains only the single-user worklog model`() {
        dataSource.connection.use { connection ->
            val tables = buildSet {
                connection.metaData.getTables(null, connection.schema, "%", arrayOf("TABLE")).use { result ->
                    while (result.next()) add(result.getString("TABLE_NAME").lowercase())
                }
            }

            assertThat(tables).contains("workday", "time_entry", "snapshot", "setting", "event")
            assertThat(tables).doesNotContain("users", "user_role", "role", "account", "refresh_session")

            val workdayColumns = buildSet {
                connection.metaData.getColumns(null, null, "WORKDAY", "%").use { result ->
                    while (result.next()) add(result.getString("COLUMN_NAME").lowercase())
                }
            }
            assertThat(workdayColumns).doesNotContain("user_id")
        }
    }
}
