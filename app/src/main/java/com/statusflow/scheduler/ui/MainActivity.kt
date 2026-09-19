package com.statusflow.scheduler.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.statusflow.scheduler.ui.screens.StatusFlowAppRoot
import com.statusflow.scheduler.ui.theme.StatusFlowTheme

class MainActivity : ComponentActivity() {

    private var permissionHint by mutableStateOf<String?>(null)

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRuntimePermissions()

        setContent {
            StatusFlowTheme {
                val vm: ScheduleViewModel = viewModel(
                    factory = ScheduleViewModel.factory(application)
                )
                StatusFlowAppRoot(
                    viewModel = vm,
                    permissionHint = permissionHint,
                    onOpenAccessibility = { openAccessibilitySettings() },
                    onOpenExactAlarm = { openExactAlarmSettings() },
                    onOpenBattery = { openBatterySettings() },
                    refreshHints = { refreshPermissionHints() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionHints()
    }

    private fun requestRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun refreshPermissionHints() {
        val hints = mutableListOf<String>()
        val am = getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am != null && !am.canScheduleExactAlarms()) {
            hints += "Allow exact alarms so schedules fire on time while locked."
        }
        if (!isIgnoringBatteryOptimizations()) {
            hints += "Disable battery optimization so StatusFlow can wake after lock / reboot."
        }
        if (!isAccessibilityEnabled()) {
            hints += "Enable StatusFlow Accessibility to auto-tap Send in WhatsApp."
        }
        permissionHint = hints.firstOrNull()
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(PowerManager::class.java) ?: return true
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(packageName)
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun openBatterySettings() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        runCatching { startActivity(intent) }
    }
}
