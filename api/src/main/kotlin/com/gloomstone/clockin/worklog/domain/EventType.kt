package com.gloomstone.clockin.worklog.domain

enum class EventType(val value: String) {
    NONE("none"),
    SICK("sick"),
    VACATION("vacation"),
    OTHER("other");

    companion object {
        fun fromValue(value: String): EventType {
            for (v in entries) {
                if (v.value == value) {
                    return v
                }
            }
            return NONE
        }
    }
}
