package com.statusflow.scheduler.whatsapp

import android.accessibilityservice.AccessibilityService
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val prefs = getSharedPreferences(WhatsAppSender.PREFS, MODE_PRIVATE)
        if (!prefs.getBoolean(WhatsAppSender.KEY_ARMED, false)) return

        val root = rootInActiveWindow ?: return
        val packageName = event.packageName?.toString().orEmpty()
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") return

        val mode = prefs.getString(WhatsAppSender.KEY_MODE, "message") ?: "message"
        val clicked = when (mode) {
            "status" -> clickStatusPublish(root) || clickByContentDescription(root, listOf("Send", "send", "Post", "post"))
            else -> clickSend(root)
        }

        if (clicked) {
            val scheduleId = prefs.getLong(WhatsAppSender.KEY_SCHEDULE_ID, -1L)
            WhatsAppSender.disarm(this)
            if (scheduleId >= 0) {
                CoroutineScope(Dispatchers.IO).launch {
                    StatusFlowApp.instance.repository.markStatus(scheduleId, ScheduleStatus.SENT)
                }
            }
        }
    }

    private fun clickSend(root: AccessibilityNodeInfo): Boolean {
        val candidates = listOf("Send", "send", "Envoyer", "Enviar", "Invia", "Senden")
        if (clickByContentDescription(root, candidates)) return true
        if (clickByText(root, candidates)) return true
        // Common WhatsApp send button resource ids across versions
        val ids = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp:id/conversation_entry_action_button",
            "com.whatsapp.w4b:id/send",
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
        return false
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
}
