package com.tracker.gps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tracker.gps.shared.model.TrackSession
import java.text.SimpleDateFormat
import java.util.*

class SessionHistoryAdapter(
    private val sessions: List<TrackSession>
) : RecyclerView.Adapter<SessionHistoryAdapter.SessionViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvMaxSpeed: TextView = view.findViewById(R.id.tvMaxSpeed)
        val tvAvgSpeed: TextView = view.findViewById(R.id.tvAvgSpeed)
        val tvDistance: TextView = view.findViewById(R.id.tvDistance)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvGroupName: TextView = view.findViewById(R.id.tvGroupName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        val context = holder.itemView.context

        // Format date and time
        holder.tvDate.text = dateFormat.format(Date(session.startTime))
        holder.tvTime.text = timeFormat.format(Date(session.startTime))

        // Max speed with location info
        val maxSpeedText = String.format(Locale.getDefault(), "%.1f km/h", session.maxSpeed)
        holder.tvMaxSpeed.text = maxSpeedText

        // Average speed
        holder.tvAvgSpeed.text = String.format(
            context.getString(R.string.avg_speed_format),
            session.avgSpeed
        )

        // Distance in kilometers
        val distanceKm = session.totalDistance / 1000.0
        holder.tvDistance.text = String.format(
            context.getString(R.string.distance_format),
            distanceKm
        )

        // Duration
        val durationMinutes = (session.duration / 1000 / 60).toInt()
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        holder.tvDuration.text = if (hours > 0) {
            String.format(context.getString(R.string.duration_format_hours), hours, minutes)
        } else {
            String.format(context.getString(R.string.duration_format_minutes), minutes)
        }

        // Location (if available)
        if (session.maxSpeedLatitude != null && session.maxSpeedLongitude != null) {
            holder.tvLocation.text = String.format(
                context.getString(R.string.location_format),
                session.maxSpeedLatitude,
                session.maxSpeedLongitude
            )
            holder.tvLocation.visibility = View.VISIBLE
        } else {
            holder.tvLocation.visibility = View.GONE
        }

        // Group name
        holder.tvGroupName.text = session.groupName
    }

    override fun getItemCount(): Int = sessions.size
}
