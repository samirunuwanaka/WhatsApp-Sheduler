package com.statusflow.scheduler.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.statusflow.scheduler.StatusFlowApp
import com.statusflow.scheduler.data.ScheduleEntity
import com.statusflow.scheduler.data.ScheduleStatus
import com.statusflow.scheduler.data.ScheduleType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScheduleViewModel(app: Application) : AndroidViewModel(app) {
    private val statusApp = app as StatusFlowApp
    private val repository = statusApp.repository
    private val alarms = statusApp.alarmScheduler

    val schedules = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(
        existingId: Long?,
        type: ScheduleType,
        title: String,
        message: String,
        caption: String,
        mediaUri: String?,
        phone: String,
        startMillis: Long,
        endMillis: Long
    ) {
        viewModelScope.launch {
            val entity = ScheduleEntity(
                id = existingId ?: 0L,
                type = type,
                title = title.ifBlank { if (type == ScheduleType.STATUS) "Status" else "Message" },
                message = message.trim(),
                caption = caption.trim(),
                mediaUri = mediaUri,
                phoneNumber = phone.filter { it.isDigit() || it == '+' }.filter { it.isDigit() },
                scheduleStartMillis = startMillis,
                scheduleEndMillis = endMillis,
                status = ScheduleStatus.PENDING
            )
            val id = repository.upsert(entity)
            val saved = repository.get(id) ?: return@launch
            alarms.cancel(id)
            alarms.schedule(saved)
        }
    }

    fun delete(entity: ScheduleEntity) {
        viewModelScope.launch {
            alarms.cancel(entity.id)
            repository.delete(entity)
        }
    }

    fun cancel(entity: ScheduleEntity) {
        viewModelScope.launch {
            alarms.cancel(entity.id)
            repository.markStatus(entity.id, ScheduleStatus.CANCELLED)
        }
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ScheduleViewModel(app) as T
                }
            }
    }
}
