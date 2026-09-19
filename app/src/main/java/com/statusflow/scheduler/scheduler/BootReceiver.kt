package com.statusflow.scheduler.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.statusflow.scheduler.StatusFlowApp
import com.statusflow.scheduler.data.ScheduleStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * After reboot (phone was off), re-arm future alarms and immediately dispatch
 * any schedule whose start has passed but end window is still open.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as StatusFlowApp
                val now = System.currentTimeMillis()
                val candidates = app.repository.catchUpCandidates(now)
                for (item in candidates) {
                    when {
                        now < item.scheduleStartMillis -> {
                            app.alarmScheduler.schedule(item)
                        }
                        now in item.scheduleStartMillis..item.scheduleEndMillis -> {
                            app.repository.markStatus(item.id, ScheduleStatus.DISPATCHING)
                            DispatchForegroundService.start(context, item.id)
                        }
                        else -> {
                            app.repository.markStatus(item.id, ScheduleStatus.MISSED)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
