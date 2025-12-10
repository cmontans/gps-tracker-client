package com.tracker.gps.shared.db

import androidx.room.*
import com.tracker.gps.shared.model.TrackPoint
import com.tracker.gps.shared.model.TrackSession
import com.tracker.gps.shared.model.UserData
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    // Track Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TrackSession)

    @Update
    suspend fun updateSession(session: TrackSession)

    @Query("SELECT * FROM track_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<TrackSession>>

    @Query("SELECT * FROM track_sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: String): TrackSession?

    @Query("DELETE FROM track_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    // Track Points
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoint(point: TrackPoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoints(points: List<TrackPoint>)

    @Query("SELECT * FROM track_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getTrackPoints(sessionId: String): Flow<List<TrackPoint>>

    @Query("SELECT * FROM track_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getTrackPointsSync(sessionId: String): List<TrackPoint>

    @Query("DELETE FROM track_points WHERE sessionId = :sessionId")
    suspend fun deleteTrackPoints(sessionId: String)

    @Query("DELETE FROM track_points WHERE timestamp < :before")
    suspend fun deleteOldTrackPoints(before: Long)

    // Users
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserData>)

    @Query("SELECT * FROM users WHERE groupName = :groupName ORDER BY userName ASC")
    fun getUsersByGroup(groupName: String): Flow<List<UserData>>

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUser(userId: String): UserData?

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUser(userId: String)

    @Query("DELETE FROM users WHERE groupName = :groupName")
    suspend fun deleteUsersByGroup(groupName: String)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
