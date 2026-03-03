package com.tracker.gps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tracker.gps.model.Waypoint

class WaypointsAdapter(
    private val onGoTo: (Waypoint) -> Unit,
    private val onDelete: (Waypoint) -> Unit
) : RecyclerView.Adapter<WaypointsAdapter.WaypointViewHolder>() {

    private var waypoints = listOf<Waypoint>()

    fun updateWaypoints(newWaypoints: List<Waypoint>) {
        waypoints = newWaypoints
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WaypointViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_waypoint, parent, false)
        return WaypointViewHolder(view)
    }

    override fun onBindViewHolder(holder: WaypointViewHolder, position: Int) {
        holder.bind(waypoints[position])
    }

    override fun getItemCount(): Int = waypoints.size

    inner class WaypointViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvWaypointName)
        private val tvDesc: TextView = itemView.findViewById(R.id.tvWaypointDescription)
        private val tvCoords: TextView = itemView.findViewById(R.id.tvWaypointCoords)
        private val btnGoTo: Button = itemView.findViewById(R.id.btnGoToWaypoint)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteWaypoint)

        fun bind(waypoint: Waypoint) {
            tvName.text = waypoint.name
            
            if (waypoint.description.isNullOrEmpty()) {
                tvDesc.visibility = View.GONE
            } else {
                tvDesc.visibility = View.VISIBLE
                tvDesc.text = waypoint.description
            }
            
            tvCoords.text = String.format("%.5f, %.5f", waypoint.latitude, waypoint.longitude)
            
            btnGoTo.setOnClickListener { onGoTo(waypoint) }
            btnDelete.setOnClickListener { onDelete(waypoint) }
        }
    }
}
