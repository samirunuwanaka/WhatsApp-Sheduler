package com.statusflow.scheduler.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.statusflow.scheduler.data.ScheduleEntity
import com.statusflow.scheduler.data.ScheduleStatus
import com.statusflow.scheduler.data.ScheduleType
import com.statusflow.scheduler.ui.ScheduleViewModel
import com.statusflow.scheduler.ui.theme.DeepCanopy
import com.statusflow.scheduler.ui.theme.Ember
import com.statusflow.scheduler.ui.theme.ForestNight
import com.statusflow.scheduler.ui.theme.Leaf
import com.statusflow.scheduler.ui.theme.Mist
import com.statusflow.scheduler.ui.theme.SoftMoss
import com.statusflow.scheduler.ui.theme.WarmSand
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusFlowAppRoot(
    viewModel: ScheduleViewModel,
    permissionHint: String?,
    onOpenAccessibility: () -> Unit,
    onOpenExactAlarm: () -> Unit,
    onOpenBattery: () -> Unit,
    refreshHints: () -> Unit
) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    var editorTarget by remember { mutableStateOf<ScheduleEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showSetup by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ForestNight, DeepCanopy, ForestNight)
                )
            )
    ) {
        // Soft atmospheric orbs
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp)
                .size(220.dp)
                .background(
                    Brush.radialGradient(listOf(Leaf.copy(alpha = 0.25f), ForestNight.copy(alpha = 0f))),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 80.dp)
                .size(260.dp)
                .background(
                    Brush.radialGradient(listOf(WarmSand.copy(alpha = 0.12f), ForestNight.copy(alpha = 0f))),
                    CircleShape
                )
        )

        if (showEditor) {
            ScheduleEditorScreen(
                initial = editorTarget,
                onDismiss = { showEditor = false },
                onSave = { id, type, title, message, caption, mediaUri, phone, start, end ->
                    viewModel.save(id, type, title, message, caption, mediaUri, phone, start, end)
                    showEditor = false
                }
            )
        } else {
            HomeScreen(
                schedules = schedules,
                permissionHint = permissionHint,
                onAdd = {
                    editorTarget = null
                    showEditor = true
                },
                onEdit = {
                    editorTarget = it
                    showEditor = true
                },
                onDelete = viewModel::delete,
                onCancel = viewModel::cancel,
                onOpenSetup = { showSetup = true }
            )
        }

        AnimatedVisibility(
            visible = showSetup,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut()
        ) {
            SetupSheet(
                onDismiss = {
                    showSetup = false
                    refreshHints()
                },
                onOpenAccessibility = onOpenAccessibility,
                onOpenExactAlarm = onOpenExactAlarm,
                onOpenBattery = onOpenBattery
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    schedules: List<ScheduleEntity>,
    permissionHint: String?,
    onAdd: () -> Unit,
    onEdit: (ScheduleEntity) -> Unit,
    onDelete: (ScheduleEntity) -> Unit,
    onCancel: (ScheduleEntity) -> Unit,
    onOpenSetup: () -> Unit
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = Leaf,
                contentColor = ForestNight,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New schedule")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "StatusFlow",
                        style = MaterialTheme.typography.displayLarge,
                        color = Mist
                    )
                    Text(
                        text = "Schedule WhatsApp messages & status — even offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftMoss,
                        modifier = Modifier.padding(top = 6.dp, end = 24.dp)
                    )
                }
                IconButton(onClick = onOpenSetup) {
                    Icon(Icons.Default.Settings, contentDescription = "Setup", tint = Mist)
                }
            }

            AnimatedVisibility(visible = permissionHint != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .clickable(onClick = onOpenSetup),
                    color = WarmSand.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = permissionHint.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmSand,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = "Upcoming & recent",
                style = MaterialTheme.typography.titleLarge,
                color = Mist
            )
            Spacer(Modifier.height(12.dp))

            if (schedules.isEmpty()) {
                EmptyState(onAdd = onAdd)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(schedules, key = { it.id }) { item ->
                        ScheduleRow(
                            item = item,
                            onClick = { onEdit(item) },
                            onDelete = { onDelete(item) },
                            onCancel = { onCancel(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No schedules yet",
            style = MaterialTheme.typography.headlineMedium,
            color = Mist
        )
        Text(
            text = "Pick a start and an end window. If the phone was off, StatusFlow catches up before the end time.",
            style = MaterialTheme.typography.bodyMedium,
            color = SoftMoss,
            modifier = Modifier.padding(top = 10.dp, start = 12.dp, end = 12.dp)
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = Leaf, contentColor = ForestNight),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Create first schedule")
        }
    }
}

@Composable
private fun ScheduleRow(
    item: ScheduleEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("EEE, d MMM · HH:mm", Locale.getDefault()) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (item.type == ScheduleType.STATUS) Icons.Default.Campaign else Icons.Default.Chat,
                    contentDescription = null,
                    tint = Leaf,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Mist,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StatusChip(item.status)
            }
            Text(
                text = if (item.type == ScheduleType.STATUS) {
                    item.caption.ifBlank { if (item.mediaUri != null) "Photo status" else item.message }
                } else {
                    item.message
                },
                style = MaterialTheme.typography.bodyMedium,
                color = SoftMoss,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = WarmSand, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${formatter.format(Date(item.scheduleStartMillis))}  →  ${formatter.format(Date(item.scheduleEndMillis))}",
                    style = MaterialTheme.typography.labelLarge,
                    color = WarmSand
                )
            }
            if (item.status == ScheduleStatus.PENDING || item.status == ScheduleStatus.FAILED || item.status == ScheduleStatus.DISPATCHING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = SoftMoss)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Ember)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: ScheduleStatus) {
    val (label, color) = when (status) {
        ScheduleStatus.PENDING -> "Pending" to Leaf
        ScheduleStatus.DISPATCHING -> "Sending" to WarmSand
        ScheduleStatus.SENT -> "Sent" to SoftMoss
        ScheduleStatus.MISSED -> "Missed" to Ember
        ScheduleStatus.FAILED -> "Retry" to Ember
        ScheduleStatus.CANCELLED -> "Cancelled" to SoftMoss
    }
    Surface(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SetupSheet(
    onDismiss: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenExactAlarm: () -> Unit,
    onOpenBattery: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestNight.copy(alpha = 0.72f))
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            color = DeepCanopy,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Make it reliable", style = MaterialTheme.typography.headlineMedium, color = Mist)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Mist)
                    }
                }
                Text(
                    "These system settings let schedules run while locked, after reboot, and auto-send in WhatsApp.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftMoss,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                SetupAction("Exact alarms", "Fire at schedule start even in Doze", onOpenExactAlarm)
                SetupAction("Battery unrestricted", "Survive lock screen & reboot catch-up", onOpenBattery)
                SetupAction("Accessibility", "Auto-tap WhatsApp Send / Status", onOpenAccessibility)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SetupAction(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        color = ForestNight.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Mist)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = SoftMoss)
        }
    }
}
