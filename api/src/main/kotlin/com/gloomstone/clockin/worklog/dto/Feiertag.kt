package com.gloomstone.clockin.worklog.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate


class HolidayDto(
    @field:JsonProperty("Datum")
    var datum: LocalDate? = null,

    @field:JsonProperty("Feiertag")
    var feiertag: Feiertag? = null,
)

class Feiertag(
    @field:JsonProperty("Name")
    var name: String? = null,

    @field:JsonProperty("Laender")
    var laender: List<Land>? = null,
)

class Land(
    @field:JsonProperty("Name")
    var name: String? = null,

    @field:JsonProperty("Abkuerzung")
    var abkuerzung: String? = null,
)
