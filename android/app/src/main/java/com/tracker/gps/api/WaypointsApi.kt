package com.tracker.gps.api

import android.util.Log
import com.google.gson.Gson
import com.tracker.gps.model.Waypoint
import com.tracker.gps.model.WaypointsResponse
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class WaypointsApi(private val baseUrl: String) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TAG = "WaypointsApi"
    }

    private fun getHttpUrl(): String {
        return baseUrl.replace("ws://", "http://").replace("wss://", "https://")
    }

    fun getWaypoints(
        groupName: String,
        onSuccess: (List<Waypoint>) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "${getHttpUrl()}/api/waypoints/$groupName"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to get waypoints", e)
                onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        val body = response.body?.string()
                        val res = gson.fromJson(body, WaypointsResponse::class.java)
                        onSuccess(res.waypoints ?: emptyList())
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse waypoints response", e)
                        onError("Parse error: ${e.message}")
                    }
                } else {
                    onError("Server error: ${response.code}")
                }
                response.close()
            }
        })
    }

    fun createWaypoint(
        waypoint: Waypoint,
        onSuccess: (Waypoint) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "${getHttpUrl()}/api/waypoints"
        val json = gson.toJson(waypoint)
        val requestBody = json.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to create waypoint", e)
                onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        val body = response.body?.string()
                        // The server returns { success: true, waypoint: ... }
                        val jsonResponse = gson.fromJson(body, Map::class.java)
                        val waypointJson = gson.toJson(jsonResponse["waypoint"])
                        val created = gson.fromJson(waypointJson, Waypoint::class.java)
                        onSuccess(created)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse create waypoint response", e)
                        onError("Parse error: ${e.message}")
                    }
                } else {
                    onError("Server error: ${response.code}")
                }
                response.close()
            }
        })
    }

    fun deleteWaypoint(
        id: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "${getHttpUrl()}/api/waypoints/$id"
        val request = Request.Builder().url(url).delete().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Server error: ${response.code}")
                }
                response.close()
            }
        })
    }
}
