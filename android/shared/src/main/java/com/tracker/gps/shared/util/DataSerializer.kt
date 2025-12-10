package com.tracker.gps.shared.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Utility for serializing/deserializing data for Wearable Data Layer communication
 */
object DataSerializer {
    private val gson = Gson()

    fun <T> toJson(data: T): String {
        return gson.toJson(data)
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return gson.fromJson(json, clazz)
    }

    inline fun <reified T> fromJson(json: String): T {
        return gson.fromJson(json, object : TypeToken<T>() {}.type)
    }

    fun toBytes(data: Any): ByteArray {
        return toJson(data).toByteArray(Charsets.UTF_8)
    }

    inline fun <reified T> fromBytes(bytes: ByteArray): T {
        val json = String(bytes, Charsets.UTF_8)
        return fromJson(json)
    }
}
