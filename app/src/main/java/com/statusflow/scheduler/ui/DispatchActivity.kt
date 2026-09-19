package com.statusflow.scheduler.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.statusflow.scheduler.StatusFlowApp
import com.statusflow.scheduler.data.ScheduleStatus
import com.statusflow.scheduler.data.ScheduleType
import com.statusflow.scheduler.scheduler.AlarmScheduler
import com.statusflow.scheduler.ui.theme.DeepCanopy
import com.statusflow.scheduler.ui.theme.ForestNight
import com.statusflow.scheduler.ui.theme.Leaf
import com.statusflow.scheduler.ui.theme.StatusFlowTheme
import com.statusflow.scheduler.whatsapp.WhatsAppSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Launched over the lock screen to hand off into WhatsApp with auto-send armed.
 */
class DispatchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        val scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        setContent {
            StatusFlowTheme {
                var message by remember { mutableStateOf("Opening WhatsApp…") }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(ForestNight, DeepCanopy))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Leaf)
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                LaunchedEffect(scheduleId) {
                    if (scheduleId < 0) {
                        finish()
                        return@LaunchedEffect
                    }
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            val app = application as StatusFlowApp
                            val entity = app.repository.get(scheduleId)
                                ?: error("Schedule not found")
                            val now = System.currentTimeMillis()
                            if (now > entity.scheduleEndMillis) {
                                app.repository.markStatus(scheduleId, ScheduleStatus.MISSED)
                                error("Schedule window ended")
                            }
                            val mode = if (entity.type == ScheduleType.STATUS) "status" else "message"
                            WhatsAppSender.armAutoSend(this@DispatchActivity, scheduleId, mode)
                            WhatsAppSender.buildIntent(this@DispatchActivity, entity)
                        }
                    }
                    result.onSuccess { waIntent ->
                        startActivity(waIntent)
                        // Keep the handoff activity alive while WhatsApp creates its UI;
                        // the accessibility service continues independently in the background.
                        delay(1500)
                        finish()
                    }.onFailure { err ->
                        message = err.message ?: "Failed"
                        withContext(Dispatchers.IO) {
                            StatusFlowApp.instance.repository.markStatus(
                                scheduleId,
                                ScheduleStatus.FAILED,
                                err.message
                            )
                        }
                        delay(1500)
                        finish()
                    }
                }
            }
        }
    }
}
