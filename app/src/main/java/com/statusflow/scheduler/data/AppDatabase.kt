package com.statusflow.scheduler.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromType(value: ScheduleType): String = value.name

    @TypeConverter
    fun toType(value: String): ScheduleType = ScheduleType.valueOf(value)

    @TypeConverter
    fun fromStatus(value: ScheduleStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): ScheduleStatus = ScheduleStatus.valueOf(value)
}

@Database(entities = [ScheduleEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "statusflow.db"
                ).build().also { instance = it }
            }
        }
    }
}
