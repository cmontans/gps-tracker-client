package com.tracker.gps.db

import androidx.room.*
import com.tracker.gps.model.JumpSession
import kotlinx.coroutines.flow.Flow

@Dao
interface JumpDao {
    @Query("SELECT * FROM jump_sessions ORDER BY timestamp DESC")
    fun getAllJumps(): Flow<List<JumpSession>>

    @Insert
    suspend fun insertJump(jump: JumpSession)

    @Delete
    suspend fun deleteJump(jump: JumpSession)

    @Query("SELECT * FROM jump_sessions ORDER BY id DESC LIMIT 1")
    suspend fun getLastJump(): JumpSession?
}
