package com.tracker.gps.wear.complication

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.tracker.gps.shared.util.SpeedFormatter
import com.tracker.gps.wear.R

class SpeedComplicationService : ComplicationDataSourceService() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("gps_tracker_prefs", Context.MODE_PRIVATE)
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        val currentSpeed = sharedPreferences.getFloat("current_speed", 0f).toDouble()
        val maxSpeed = sharedPreferences.getFloat("max_speed", 0f).toDouble()

        val complicationData = when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(
                        SpeedFormatter.formatSpeed(currentSpeed)
                    ).build(),
                    contentDescription = PlainComplicationText.Builder(
                        "Current speed: ${SpeedFormatter.formatSpeedWithUnit(currentSpeed)}"
                    ).build()
                )
                    .setTitle(PlainComplicationText.Builder("km/h").build())
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(
                        "Speed: ${SpeedFormatter.formatSpeed(currentSpeed)} km/h"
                    ).build(),
                    contentDescription = PlainComplicationText.Builder(
                        "Current speed: ${SpeedFormatter.formatSpeedWithUnit(currentSpeed)}"
                    ).build()
                )
                    .setTitle(PlainComplicationText.Builder("GPS Tracker").build())
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = currentSpeed.toFloat(),
                    min = 0f,
                    max = if (maxSpeed > 0) maxSpeed.toFloat() else 100f,
                    contentDescription = PlainComplicationText.Builder(
                        "Current speed: ${SpeedFormatter.formatSpeedWithUnit(currentSpeed)}"
                    ).build()
                )
                    .setText(PlainComplicationText.Builder(
                        SpeedFormatter.formatSpeed(currentSpeed)
                    ).build())
                    .setTitle(PlainComplicationText.Builder("km/h").build())
                    .build()
            }
            else -> {
                NoDataComplicationData()
            }
        }

        listener.onComplicationData(complicationData)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("45.0").build(),
                    contentDescription = PlainComplicationText.Builder("Speed preview").build()
                )
                    .setTitle(PlainComplicationText.Builder("km/h").build())
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("Speed: 45.0 km/h").build(),
                    contentDescription = PlainComplicationText.Builder("Speed preview").build()
                )
                    .setTitle(PlainComplicationText.Builder("GPS Tracker").build())
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = 45f,
                    min = 0f,
                    max = 100f,
                    contentDescription = PlainComplicationText.Builder("Speed preview").build()
                )
                    .setText(PlainComplicationText.Builder("45.0").build())
                    .setTitle(PlainComplicationText.Builder("km/h").build())
                    .build()
            }
            else -> NoDataComplicationData()
        }
    }

    companion fun {
        fun requestUpdate(context: Context) {
            val componentName = ComponentName(context, SpeedComplicationService::class.java)
            // Request complication update
            // Note: This requires the Complications Provider API
        }

        fun updateSpeed(context: Context, currentSpeed: Double, maxSpeed: Double) {
            val prefs = context.getSharedPreferences("gps_tracker_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putFloat("current_speed", currentSpeed.toFloat())
                .putFloat("max_speed", maxSpeed.toFloat())
                .apply()
            requestUpdate(context)
        }
    }
}
