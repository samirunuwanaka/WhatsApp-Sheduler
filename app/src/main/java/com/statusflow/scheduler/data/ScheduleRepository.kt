package com.statusflow.scheduler.data

import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val dao: ScheduleDao) {
    fun observeAll(): Flow<List<ScheduleEntity>> = dao.observeAll()

    suspend fun get(id: Long): ScheduleEntity? = dao.getById(id)

    suspend fun upsert(entity: ScheduleEntity): Long {
        return if (entity.id == 0L) dao.insert(entity) else {
            dao.update(entity)
            entity.id
        }
    }

    suspend fun delete(entity: ScheduleEntity) = dao.delete(entity)

    suspend fun markStatus(id: Long, status: ScheduleStatus, error: String? = null) {
        val current = dao.getById(id) ?: return
        dao.update(
            current.copy(
                status = status,
                lastError = error,
                completedAtMillis = if (
                    status == ScheduleStatus.SENT ||
                    status == ScheduleStatus.MISSED ||
                    status == ScheduleStatus.CANCELLED
                ) System.currentTimeMillis() else current.completedAtMillis
            )
        )
    }

    suspend fun catchUpCandidates(now: Long = System.currentTimeMillis()): List<ScheduleEntity> =
        dao.getCatchUpCandidates(now)
}
