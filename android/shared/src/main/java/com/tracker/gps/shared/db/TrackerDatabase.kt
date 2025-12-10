package com.tracker.gps.shared.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tracker.gps.shared.model.TrackPoint
import com.tracker.gps.shared.model.TrackSession
import com.tracker.gps.shared.model.UserData

@Database(
    entities = [
        UserData::class,
        TrackPoint::class,
        TrackSession::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile
        private var INSTANCE: TrackerDatabase? = null

        fun getInstance(context: Context): TrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackerDatabase::class.java,
                    "gps_tracker_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
