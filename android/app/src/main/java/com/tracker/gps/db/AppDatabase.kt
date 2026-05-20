package com.tracker.gps.db

import android.content.Context
import androidx.room.*
import com.tracker.gps.model.JumpSession

@Database(entities = [JumpSession::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jumpDao(): JumpDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gps_tracker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
