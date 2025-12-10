package com.tracker.gps.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

data class SpeedHistoryRecord(
    @SerializedName("userId") val userId: String,
    @SerializedName("userName") val userName: String,
    @SerializedName("groupName") val groupName: String,
    @SerializedName("maxSpeed") val maxSpeed: Double,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timestamp") val timestamp: Long
)

data class SpeedHistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("userId") val userId: String?,
    @SerializedName("count") val count: Int?,
    @SerializedName("records") val records: List<SpeedHistoryRecordDb>?
)

data class SpeedHistoryRecordDb(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: String,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("group_name") val groupName: String?,
    @SerializedName("max_speed") val maxSpeed: String,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("date") val date: String,
    @SerializedName("time") val time: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("created_at") val createdAt: String?
)

data class SpeedStatisticsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("userId") val userId: String,
    @SerializedName("statistics") val statistics: SpeedStatistics
)

data class SpeedStatistics(
    @SerializedName("total_records") val totalRecords: String,
    @SerializedName("highest_speed") val highestSpeed: String?,
    @SerializedName("average_max_speed") val averageMaxSpeed: String?,
    @SerializedName("first_record_date") val firstRecordDate: String?,
    @SerializedName("last_record_date") val lastRecordDate: String?
)

class SpeedHistoryApi(private val baseUrl: String) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TAG = "SpeedHistoryApi"
    }

    fun submitSpeedHistory(
        record: SpeedHistoryRecord,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val json = gson.toJson(record)
        val requestBody = json.toRequestBody(JSON_MEDIA_TYPE)

        val httpUrl = baseUrl.replace("ws://", "http://").replace("wss://", "https://")
        val url = "$httpUrl/api/speed-history"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to submit speed history", e)
                onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Speed history submitted successfully")
                    onSuccess()
                } else {
                    Log.e(TAG, "Failed to submit speed history: ${response.code}")
                    onError("Server error: ${response.code}")
                }
                response.close()
            }
        })
    }

    fun getSpeedHistory(
        userId: String,
        limit: Int = 100,
        offset: Int = 0,
        onSuccess: (List<SpeedHistoryRecordDb>) -> Unit,
        onError: (String) -> Unit
    ) {
        val httpUrl = baseUrl.replace("ws://", "http://").replace("wss://", "https://")
        val url = "$httpUrl/api/speed-history/$userId?limit=$limit&offset=$offset"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to get speed history", e)
                onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        val responseBody = response.body?.string()
                        val historyResponse = gson.fromJson(responseBody, SpeedHistoryResponse::class.java)
                        Log.d(TAG, "Speed history retrieved: ${historyResponse.count} records")
                        onSuccess(historyResponse.records ?: emptyList())
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse speed history response", e)
                        onError("Parse error: ${e.message}")
                    }
                } else {
                    Log.e(TAG, "Failed to get speed history: ${response.code}")
                    onError("Server error: ${response.code}")
                }
                response.close()
            }
        })
    }

    fun getSpeedStatistics(
        userId: String,
        onSuccess: (SpeedStatistics) -> Unit,
        onError: (String) -> Unit
    ) {
        val httpUrl = baseUrl.replace("ws://", "http://").replace("wss://", "https://")
        val url = "$httpUrl/api/speed-history/$userId/stats"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to get speed statistics", e)
                onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        val responseBody = response.body?.string()
                        val statsResponse = gson.fromJson(responseBody, SpeedStatisticsResponse::class.java)
                        Log.d(TAG, "Speed statistics retrieved")
                        onSuccess(statsResponse.statistics)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse statistics response", e)
                        onError("Parse error: ${e.message}")
                    }
                } else {
                    Log.e(TAG, "Failed to get statistics: ${response.code}")
                    onError("Server error: ${response.code}")
                }
                response.close()
            }
        })
    }
}
