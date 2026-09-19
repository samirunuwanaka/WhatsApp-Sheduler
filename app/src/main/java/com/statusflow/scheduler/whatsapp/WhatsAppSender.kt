package com.statusflow.scheduler.whatsapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.content.ClipData
import com.statusflow.scheduler.data.ScheduleEntity
import com.statusflow.scheduler.data.ScheduleType

object WhatsAppSender {
    const val PREFS = "statusflow_dispatch"
    const val KEY_ARMED = "auto_send_armed"
    const val KEY_SCHEDULE_ID = "auto_send_schedule_id"
    const val KEY_MODE = "auto_send_mode"

    fun resolvePackage(context: Context): String? {
        val pm = context.packageManager
        return listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun armAutoSend(context: Context, scheduleId: Long, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ARMED, true)
            .putLong(KEY_SCHEDULE_ID, scheduleId)
            .putString(KEY_MODE, mode)
            .apply()
    }

    fun disarm(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ARMED, false)
            .apply()
    }

    fun buildIntent(context: Context, entity: ScheduleEntity): Intent {
        val pkg = resolvePackage(context)
            ?: throw IllegalStateException("WhatsApp is not installed")

        return when (entity.type) {
            ScheduleType.MESSAGE -> {
                val phone = entity.phoneNumber.filter { it.isDigit() }
                if (phone.isBlank()) throw IllegalStateException("Phone number required")
                val uri = Uri.parse(
                    "https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(entity.message)}"
                )
                Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(pkg)
                       addFlags(
                           Intent.FLAG_ACTIVITY_NEW_TASK or
                               Intent.FLAG_ACTIVITY_CLEAR_TOP or
                               Intent.FLAG_ACTIVITY_SINGLE_TOP
                       )
                }
            }
            ScheduleType.STATUS -> {
                Intent(Intent.ACTION_SEND).apply {
                    val media = entity.mediaUri?.let(Uri::parse)
                    type = if (media != null) {
                        context.contentResolver.getType(media) ?: "image/*"
                    } else {
                        "text/plain"
                    }
                    if (media != null) {
                        putExtra(Intent.EXTRA_STREAM, media)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        clipData = ClipData.newRawUri("status-image", media)
                    }
                    val caption = entity.caption.ifBlank { entity.message }
                    if (caption.isNotBlank()) putExtra(Intent.EXTRA_TEXT, caption)
                    setPackage(pkg)
                       addFlags(
                           Intent.FLAG_ACTIVITY_NEW_TASK or
                               Intent.FLAG_ACTIVITY_CLEAR_TOP or
                               Intent.FLAG_ACTIVITY_SINGLE_TOP
                       )
                    // WhatsApp status share target when available
                    putExtra("jid", "status@broadcast")
                }
            }
        }
    }
}
