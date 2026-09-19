package com.statusflow.scheduler.data

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Database(entities = [ScheduleEntity::class], version = 2, exportSchema = false)
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
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE schedules ADD COLUMN caption TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE schedules ADD COLUMN mediaUri TEXT")
                database.execSQL("UPDATE schedules SET caption = message WHERE type = 'STATUS'")
            }
        }
    }
}
