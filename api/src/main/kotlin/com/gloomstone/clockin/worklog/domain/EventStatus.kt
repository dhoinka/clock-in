package com.gloomstone.clockin.worklog.domain

enum class EventStatus(val value: String) {
    NEW("new"),
    APPROVED("approved");

    override fun toString(): String {
        return value
    }

    companion object {
        fun fromValue(value: String): EventStatus {
            for (v in entries) {
                if (v.value == value) {
                    return v
                }
            }
            return NEW
        }
    }
}
