package com.statusflow.scheduler.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ScheduleType,
    val title: String,
    val message: String,
    /** Digits with country code, no + or spaces. Empty for status. */
    val phoneNumber: String = "",
    /** Epoch millis when the window opens. */
    val scheduleStartMillis: Long,
    /** Epoch millis when the window closes. Catch-up allowed until this time. */
    val scheduleEndMillis: Long,
    val status: ScheduleStatus = ScheduleStatus.PENDING,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastError: String? = null,
    val completedAtMillis: Long? = null
)
