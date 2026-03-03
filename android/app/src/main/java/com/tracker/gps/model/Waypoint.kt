package com.tracker.gps.model

import com.google.gson.annotations.SerializedName

data class Waypoint(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("group_name") val groupName: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("created_by") val createdBy: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class WaypointsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("groupName") val groupName: String,
    @SerializedName("count") val count: Int,
    @SerializedName("waypoints") val waypoints: List<Waypoint>?
)
