package com.tracker.gps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tracker.gps.api.SpeedHistoryRecordDb

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private var records = listOf<SpeedHistoryRecordDb>()

    fun updateRecords(newRecords: List<SpeedHistoryRecordDb>) {
        records = newRecords
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_record, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvMaxSpeed: TextView = itemView.findViewById(R.id.tvMaxSpeed)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvGroupName: TextView = itemView.findViewById(R.id.tvGroupName)

        fun bind(record: SpeedHistoryRecordDb) {
            tvDate.text = record.date
            tvTime.text = record.time
            tvMaxSpeed.text = String.format("%.1f km/h", record.maxSpeed.toDouble())
            tvLocation.text = String.format("%.4f, %.4f",
                record.latitude.toDouble(),
                record.longitude.toDouble())

            if (!record.groupName.isNullOrEmpty() && record.groupName != "default") {
                tvGroupName.visibility = View.VISIBLE
                tvGroupName.text = "Group: ${record.groupName}"
            } else {
                tvGroupName.visibility = View.GONE
            }
        }
    }
}
