package com.tracker.gps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tracker.gps.api.SpeedHistoryRecordDb

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private var records = listOf<SpeedHistoryRecordDb>()
    private var speedUnit: String = "kmh"

    fun updateRecords(newRecords: List<SpeedHistoryRecordDb>) {
        records = newRecords
        notifyDataSetChanged()
    }

    fun setSpeedUnit(unit: String) {
        speedUnit = unit
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_record, parent, false)
        return HistoryViewHolder(view, speedUnit)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    class HistoryViewHolder(itemView: View, private val speedUnit: String) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvMaxSpeed: TextView = itemView.findViewById(R.id.tvMaxSpeed)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvGroupName: TextView = itemView.findViewById(R.id.tvGroupName)
        private val layoutWindowedSpeeds: View = itemView.findViewById(R.id.layoutWindowedSpeeds)
        private val tvMax10sHistory: TextView = itemView.findViewById(R.id.tvMax10s)
        private val tvMax500mHistory: TextView = itemView.findViewById(R.id.tvMax500m)

        private fun convertSpeed(speedInKmh: Double): Double {
            return when (speedUnit) {
                "mph" -> speedInKmh * com.tracker.gps.shared.util.Constants.KMH_TO_MPH
                "knots" -> speedInKmh * com.tracker.gps.shared.util.Constants.KMH_TO_KNOTS
                else -> speedInKmh
            }
        }

        private fun getUnitLabel(): String {
            return when (speedUnit) {
                "mph" -> "mph"
                "knots" -> "knots"
                else -> "km/h"
            }
        }

        fun bind(record: SpeedHistoryRecordDb) {
            val unit = getUnitLabel()
            tvDate.text = record.date
            tvTime.text = record.time
            tvMaxSpeed.text = String.format("%.1f %s", convertSpeed(record.maxSpeed.toDouble()), unit)
            tvLocation.text = String.format("%.4f, %.4f",
                record.latitude.toDouble(),
                record.longitude.toDouble())

            if (!record.groupName.isNullOrEmpty() && record.groupName != "default") {
                tvGroupName.visibility = View.VISIBLE
                tvGroupName.text = "Group: ${record.groupName}"
            } else {
                tvGroupName.visibility = View.GONE
            }

            if (!record.maxSpeed10s.isNullOrEmpty() || !record.maxSpeed500m.isNullOrEmpty()) {
                layoutWindowedSpeeds.visibility = View.VISIBLE
                val speed10s = record.maxSpeed10s?.toDoubleOrNull() ?: 0.0
                val speed500m = record.maxSpeed500m?.toDoubleOrNull() ?: 0.0
                tvMax10sHistory.text = "10s: ${String.format("%.1f", convertSpeed(speed10s))} $unit"
                tvMax500mHistory.text = "500m: ${String.format("%.1f", convertSpeed(speed500m))} $unit"
            } else {
                layoutWindowedSpeeds.visibility = View.GONE
            }
        }
    }
}
