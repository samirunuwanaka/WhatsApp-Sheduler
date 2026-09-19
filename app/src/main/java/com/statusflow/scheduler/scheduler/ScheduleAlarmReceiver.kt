package com.statusflow.scheduler.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != AlarmScheduler.ACTION_FIRE) return
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        if (id < 0) return
        DispatchForegroundService.start(context, id)
    }
}
