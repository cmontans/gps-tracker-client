package com.tracker.gps.shared.api

import com.google.gson.Gson
import com.tracker.gps.shared.model.TrackSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * API service for communicating with the GPS tracker server
 */
class SessionApiService(private val baseUrl: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Save a completed session to the server
     */
    suspend fun saveSession(session: TrackSession): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(session)
            val requestBody = json.toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$baseUrl/api/sessions")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val apiResponse = gson.fromJson(responseBody, ApiResponse::class.java)
                    if (apiResponse.success) {
                        Result.success(apiResponse.sessionId ?: session.sessionId)
                    } else {
                        Result.failure(Exception(apiResponse.error ?: "Unknown error"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get session history for a user
     */
    suspend fun getSessionHistory(
        userId: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<TrackSession>> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/sessions?userId=$userId&limit=$limit&offset=$offset"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val apiResponse = gson.fromJson(responseBody, SessionsResponse::class.java)
                    if (apiResponse.success) {
                        Result.success(apiResponse.sessions)
                    } else {
                        Result.failure(Exception(apiResponse.error ?: "Unknown error"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a specific session by ID
     */
    suspend fun getSession(sessionId: String): Result<TrackSession> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/sessions/$sessionId")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val apiResponse = gson.fromJson(responseBody, SessionResponse::class.java)
                    if (apiResponse.success && apiResponse.session != null) {
                        Result.success(apiResponse.session)
                    } else {
                        Result.failure(Exception(apiResponse.error ?: "Unknown error"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Response data classes
    private data class ApiResponse(
        val success: Boolean,
        val sessionId: String? = null,
        val message: String? = null,
        val error: String? = null
    )

    private data class SessionsResponse(
        val success: Boolean,
        val count: Int = 0,
        val sessions: List<TrackSession> = emptyList(),
        val error: String? = null
    )

    private data class SessionResponse(
        val success: Boolean,
        val session: TrackSession? = null,
        val error: String? = null
    )
}
