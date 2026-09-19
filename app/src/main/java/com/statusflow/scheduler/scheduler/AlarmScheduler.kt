package com.statusflow.scheduler.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.statusflow.scheduler.data.ScheduleEntity
import com.statusflow.scheduler.data.ScheduleStatus

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(entity: ScheduleEntity) {
        if (entity.status != ScheduleStatus.PENDING && entity.status != ScheduleStatus.FAILED) return
        val triggerAt = entity.scheduleStartMillis
        if (triggerAt <= 0L) return

        val pending = pendingIntent(entity.id)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }

    fun cancel(id: Long) {
        alarmManager.cancel(pendingIntent(id))
    }

    private fun pendingIntent(id: Long): PendingIntent {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_SCHEDULE_ID, id)
        }
        return PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_FIRE = "com.statusflow.scheduler.ACTION_FIRE"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
    }
}
