package com.statusflow.scheduler.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY scheduleStartMillis ASC")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: Long): ScheduleEntity?

    @Query("SELECT * FROM schedules WHERE status = :status")
    suspend fun getByStatus(status: ScheduleStatus): List<ScheduleEntity>

    @Query(
        """
        SELECT * FROM schedules
        WHERE status IN ('PENDING', 'DISPATCHING', 'FAILED')
          AND scheduleEndMillis >= :now
        ORDER BY scheduleStartMillis ASC
        """
    )
    suspend fun getCatchUpCandidates(now: Long): List<ScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScheduleEntity): Long

    @Update
    suspend fun update(entity: ScheduleEntity)

    @Delete
    suspend fun delete(entity: ScheduleEntity)
}
