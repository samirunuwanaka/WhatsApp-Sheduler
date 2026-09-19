package com.statusflow.scheduler.scheduler

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.statusflow.scheduler.R
import com.statusflow.scheduler.StatusFlowApp
import com.statusflow.scheduler.data.ScheduleStatus
import com.statusflow.scheduler.ui.DispatchActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DispatchForegroundService : Service() {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L) ?: -1L
        if (id < 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID + id.toInt(), buildNotification("Preparing WhatsApp…"))

        scope.launch {
            val app = application as StatusFlowApp
            val entity = app.repository.get(id)
            if (entity == null) {
                stopSelfSafely()
                return@launch
            }

            val now = System.currentTimeMillis()
            if (now > entity.scheduleEndMillis) {
                app.repository.markStatus(id, ScheduleStatus.MISSED, "Schedule end window passed")
                stopSelfSafely()
                return@launch
            }

            if (now < entity.scheduleStartMillis) {
                app.alarmScheduler.schedule(entity)
                stopSelfSafely()
                return@launch
            }

            if (!hasNetwork(this@DispatchForegroundService)) {
                // Stay offline-aware: retry later inside the window.
                app.repository.markStatus(id, ScheduleStatus.FAILED, "No network — will retry in window")
                scheduleRetry(id, entity.scheduleEndMillis)
                stopSelfSafely()
                return@launch
            }

            app.repository.markStatus(id, ScheduleStatus.DISPATCHING)
            Handler(Looper.getMainLooper()).post {
                val launch = Intent(this@DispatchForegroundService, DispatchActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, id)
                }
                startActivity(launch)
                stopSelfSafely()
            }
        }

        return START_NOT_STICKY
    }

    private fun scheduleRetry(id: Long, endMillis: Long) {
        val retryAt = (System.currentTimeMillis() + RETRY_MS).coerceAtMost(endMillis)
        if (retryAt <= System.currentTimeMillis()) return
        val app = application as StatusFlowApp
        scope.launch {
            val entity = app.repository.get(id) ?: return@launch
            app.alarmScheduler.schedule(
                entity.copy(
                    scheduleStartMillis = retryAt,
                    status = ScheduleStatus.PENDING
                )
            )
            // Keep original end; only nudge start for alarm purposes via temporary reschedule.
            // Persist nudged start so alarm uses it.
            app.repository.upsert(
                entity.copy(
                    scheduleStartMillis = retryAt,
                    status = ScheduleStatus.PENDING,
                    lastError = "Waiting for network inside schedule window"
                )
            )
        }
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, StatusFlowApp.CHANNEL_DISPATCH)
            .setContentTitle("StatusFlow")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    private fun stopSelfSafely() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 4200
        private const val RETRY_MS = 60_000L

        fun start(context: Context, scheduleId: Long) {
            val intent = Intent(context, DispatchForegroundService::class.java).apply {
                putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun hasNetwork(context: Context): Boolean {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
}
