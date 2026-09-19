package com.statusflow.scheduler.whatsapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.statusflow.scheduler.StatusFlowApp
import com.statusflow.scheduler.data.ScheduleStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * When StatusFlow arms a dispatch, this service looks for WhatsApp's Send button
 * (or status publish affordance) and performs a click.
 */
class AutoSendAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var attemptCount = 0
    private val retry = object : Runnable {
        override fun run() {
            if (trySend()) return
            if (attemptCount++ < MAX_ATTEMPTS) {
                handler.postDelayed(this, RETRY_DELAY_MS)
            } else {
                val scheduleId = getSharedPreferences(WhatsAppSender.PREFS, MODE_PRIVATE)
                    .getLong(WhatsAppSender.KEY_SCHEDULE_ID, -1L)
                WhatsAppSender.disarm(this@AutoSendAccessibilityService)
                if (scheduleId >= 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        StatusFlowApp.instance.repository.markStatus(
                            scheduleId,
                            ScheduleStatus.FAILED,
                            "WhatsApp send button was not found"
                        )
                    }
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val prefs = getSharedPreferences(WhatsAppSender.PREFS, MODE_PRIVATE)
        if (!prefs.getBoolean(WhatsAppSender.KEY_ARMED, false)) return

        val root = rootInActiveWindow ?: return
        val packageName = root.packageName?.toString().orEmpty()
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") return

        if (attemptCount == 0) handler.removeCallbacks(retry)
        trySend()
        if (prefs.getBoolean(WhatsAppSender.KEY_ARMED, false)) {
            handler.removeCallbacks(retry)
            handler.postDelayed(retry, RETRY_DELAY_MS)
        }
    }

    private fun trySend(): Boolean {
        val prefs = getSharedPreferences(WhatsAppSender.PREFS, MODE_PRIVATE)
        if (!prefs.getBoolean(WhatsAppSender.KEY_ARMED, false)) return true
        val root = rootInActiveWindow ?: return false
        val packageName = root.packageName?.toString().orEmpty()
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") return false
        val mode = prefs.getString(WhatsAppSender.KEY_MODE, "message") ?: "message"
        val clicked = when (mode) {
            "status" -> clickStatusPublish(root) || clickByContentDescription(root, listOf("Send", "send", "Post", "post"))
            else -> clickSend(root)
        }
        if (!clicked) return false

        val scheduleId = prefs.getLong(WhatsAppSender.KEY_SCHEDULE_ID, -1L)
        handler.removeCallbacks(retry)
        WhatsAppSender.disarm(this)
        if (scheduleId >= 0) {
            CoroutineScope(Dispatchers.IO).launch {
                StatusFlowApp.instance.repository.markStatus(scheduleId, ScheduleStatus.SENT)
            }
        }
        return true
    }

    private fun clickSend(root: AccessibilityNodeInfo): Boolean {
        val candidates = listOf("Send", "send", "SEND", "Envoyer", "Enviar", "Invia", "Senden", "Enviar mensaje")
        if (clickByContentDescription(root, candidates)) return true
        if (clickByText(root, candidates)) return true
        // Common WhatsApp send button resource ids across versions
        val ids = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp:id/send_button",
            "com.whatsapp:id/conversation_entry_action_button",
            "com.whatsapp.w4b:id/send",
            "com.whatsapp.w4b:id/send_button",
            "com.whatsapp.w4b:id/conversation_entry_action_button"
        )
        for (id in ids) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (nodes != null) {
                for (node in nodes) {
                    if (performClick(node)) return true
                }
            }
        }
        return false
    }

    private fun clickStatusPublish(root: AccessibilityNodeInfo): Boolean {
        val ids = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp:id/status_send",
            "com.whatsapp.w4b:id/send"
        )
        for (id in ids) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (nodes != null) {
                for (node in nodes) {
                    if (performClick(node)) return true
                }
            }
        }
        return clickByText(root, listOf("My status", "Status", "My Status"))
    }

    private fun clickByContentDescription(root: AccessibilityNodeInfo, labels: List<String>): Boolean {
        return traverse(root) { node ->
            val desc = node.contentDescription?.toString().orEmpty()
            labels.any { desc.equals(it, ignoreCase = true) || desc.contains(it, ignoreCase = true) } &&
                performClick(node)
        }
    }

    private fun clickByText(root: AccessibilityNodeInfo, labels: List<String>): Boolean {
        for (label in labels) {
            val nodes = root.findAccessibilityNodeInfosByText(label) ?: continue
            for (node in nodes) {
                if (performClick(node)) return true
            }
        }
        return false
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.isEmpty || !node.isVisibleToUser) return false
        val path = Path().apply {
            moveTo(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }
        return dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
                .build(),
            null,
            null
        )
    }

    private fun traverse(node: AccessibilityNodeInfo, matcher: (AccessibilityNodeInfo) -> Boolean): Boolean {
        if (matcher(node)) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (traverse(child, matcher)) return true
        }
        return false
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val RETRY_DELAY_MS = 500L
        private const val MAX_ATTEMPTS = 120
    }
}
