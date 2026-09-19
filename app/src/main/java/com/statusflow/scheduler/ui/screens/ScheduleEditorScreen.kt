package com.statusflow.scheduler.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.statusflow.scheduler.data.ScheduleEntity
import com.statusflow.scheduler.data.ScheduleType
import com.statusflow.scheduler.ui.theme.DeepCanopy
import com.statusflow.scheduler.ui.theme.ForestNight
import com.statusflow.scheduler.ui.theme.Leaf
import com.statusflow.scheduler.ui.theme.Mist
import com.statusflow.scheduler.ui.theme.SoftMoss
import com.statusflow.scheduler.ui.theme.WarmSand
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleEditorScreen(
    initial: ScheduleEntity?,
    onDismiss: () -> Unit,
    onSave: (
        existingId: Long?,
        type: ScheduleType,
        title: String,
        message: String,
        phone: String,
        startMillis: Long,
        endMillis: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault()) }

    var type by remember { mutableStateOf(initial?.type ?: ScheduleType.MESSAGE) }
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var message by remember { mutableStateOf(initial?.message.orEmpty()) }
    var phone by remember { mutableStateOf(initial?.phoneNumber.orEmpty()) }

    val defaultStart = System.currentTimeMillis() + 15 * 60_000L
    val defaultEnd = defaultStart + 60 * 60_000L
    var startMillis by remember { mutableLongStateOf(initial?.scheduleStartMillis ?: defaultStart) }
    var endMillis by remember { mutableLongStateOf(initial?.scheduleEndMillis ?: defaultEnd) }
    var error by remember { mutableStateOf<String?>(null) }

    fun pickDateTime(current: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        onPicked(cal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ForestNight, DeepCanopy)))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Mist)
            }
            Text(
                text = if (initial == null) "New schedule" else "Edit schedule",
                style = MaterialTheme.typography.headlineMedium,
                color = Mist
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Schedule start opens the window. Schedule end is the last moment StatusFlow may still send after lock, reboot, or offline delay.",
            style = MaterialTheme.typography.bodyMedium,
            color = SoftMoss
        )

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TypeChip("Message", type == ScheduleType.MESSAGE) { type = ScheduleType.MESSAGE }
            TypeChip("Status", type == ScheduleType.STATUS) { type = ScheduleType.STATUS }
        }

        Spacer(Modifier.height(18.dp))
        FieldLabel("Title")
        SfTextField(value = title, onValueChange = { title = it }, placeholder = "Morning greeting")

        if (type == ScheduleType.MESSAGE) {
            Spacer(Modifier.height(14.dp))
            FieldLabel("Phone (country code, digits only)")
            SfTextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = "9198XXXXXXXX",
                keyboardType = KeyboardType.Phone
            )
        }

        Spacer(Modifier.height(14.dp))
        FieldLabel(if (type == ScheduleType.STATUS) "Status text" else "Message")
        SfTextField(
            value = message,
            onValueChange = { message = it },
            placeholder = "Write what should go out…",
            singleLine = false,
            minLines = 4
        )

        Spacer(Modifier.height(18.dp))
        TimeBlock(
            label = "Schedule start",
            value = formatter.format(Date(startMillis)),
            onClick = { pickDateTime(startMillis) { startMillis = it } }
        )
        Spacer(Modifier.height(12.dp))
        TimeBlock(
            label = "Schedule end",
            value = formatter.format(Date(endMillis)),
            onClick = { pickDateTime(endMillis) { endMillis = it } }
        )

        if (error != null) {
            Text(
                text = error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                when {
                    message.isBlank() -> error = "Message cannot be empty"
                    type == ScheduleType.MESSAGE && phone.filter { it.isDigit() }.length < 8 ->
                        error = "Enter a valid phone with country code"
                    endMillis <= startMillis -> error = "Schedule end must be after schedule start"
                    else -> {
                        error = null
                        onSave(
                            initial?.id,
                            type,
                            title,
                            message,
                            phone,
                            startMillis,
                            endMillis
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Leaf, contentColor = ForestNight),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Save schedule", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) Leaf else DeepCanopy,
        label = "chip"
    )
    val fg by animateColorAsState(
        if (selected) ForestNight else Mist,
        label = "chipFg"
    )
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = WarmSand,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SfTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = SoftMoss.copy(alpha = 0.7f)) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Leaf,
            unfocusedBorderColor = SoftMoss.copy(alpha = 0.35f),
            focusedTextColor = Mist,
            unfocusedTextColor = Mist,
            cursorColor = Leaf,
            focusedContainerColor = ForestNight.copy(alpha = 0.45f),
            unfocusedContainerColor = ForestNight.copy(alpha = 0.35f)
        )
    )
}

@Composable
private fun TimeBlock(label: String, value: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = ForestNight.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = WarmSand)
            Text(value, style = MaterialTheme.typography.titleMedium, color = Mist, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
