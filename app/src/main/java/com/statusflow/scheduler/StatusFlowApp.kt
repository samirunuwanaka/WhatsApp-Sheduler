package com.statusflow.scheduler

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.statusflow.scheduler.data.AppDatabase
import com.statusflow.scheduler.data.ScheduleRepository
import com.statusflow.scheduler.scheduler.AlarmScheduler

class StatusFlowApp : Application() {
    lateinit var repository: ScheduleRepository
        private set
    lateinit var alarmScheduler: AlarmScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        val db = AppDatabase.get(this)
        repository = ScheduleRepository(db.scheduleDao())
        alarmScheduler = AlarmScheduler(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_DISPATCH,
            getString(R.string.channel_dispatch),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_dispatch_desc)
            enableVibration(true)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_DISPATCH = "dispatch"
        lateinit var instance: StatusFlowApp
            private set
    }
}
